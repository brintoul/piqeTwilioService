package com.controlledthinking;

import com.controlledthinking.dao.AppointmentDAO;
import com.controlledthinking.db.Appointment;
import com.controlledthinking.db.PersonOrEntity;
import com.controlledthinking.resources.AppointmentResource;
import com.controlledthinking.client.TwilioServicesProvider;
import com.controlledthinking.dao.AlertQueueDAO;
import com.controlledthinking.dao.AlertQueueMessageDAO;
import com.controlledthinking.dao.CustomerTransactionDAO;
import com.controlledthinking.dao.PersonOrEntityDAO;
import com.controlledthinking.db.AlertMessage;
import com.controlledthinking.db.AlertQueue;
import com.controlledthinking.db.AlertQueueEntry;
import com.controlledthinking.db.AppUser;
import com.controlledthinking.db.Customer;
import com.controlledthinking.db.CustomerTransaction;
import com.controlledthinking.filter.CORSFilter;
import com.controlledthinking.filter.CreditCheckFilter;
import com.controlledthinking.service.BasicMessageResultProcessor;
import com.controlledthinking.service.MessageResultProcessor;
import com.controlledthinking.service.PersonOrEntityService;
import com.controlledthinking.resources.InputResource;
import com.controlledthinking.resources.NotificationsResource;
import com.controlledthinking.resources.OAuthResource;
import com.controlledthinking.resources.PersonEntityResource;
import com.controlledthinking.resources.QueueResource;
import com.controlledthinking.resources.AuthResource;
import com.controlledthinking.resources.MediaResource;
import com.controlledthinking.resources.MmsResource;
import com.controlledthinking.resources.TransactionResource;
import com.controlledthinking.resources.CreditResource;
import com.controlledthinking.resources.SmsWebhookResource;
import com.controlledthinking.auth.JwtAuthenticator;
import com.controlledthinking.auth.JwtUtil;
import com.controlledthinking.auth.SimpleAuthenticator;
import com.controlledthinking.auth.SimpleAuthorizer;
import com.controlledthinking.auth.User;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.auth.chained.ChainedAuthFilter;
import java.util.List;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import java.util.EnumSet;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

public class TwilioPIQEApplication extends Application<TwilioPIQEConfiguration> {

    public static void main(final String[] args) throws Exception {
        new TwilioPIQEApplication().run(args);
    }

    private final HibernateBundle<TwilioPIQEConfiguration> hibernate =
        new HibernateBundle<>(Appointment.class, PersonOrEntity.class, AlertQueueEntry.class, AlertQueue.class, AlertMessage.class, Customer.class, CustomerTransaction.class, AppUser.class) {
            @Override
            public DataSourceFactory getDataSourceFactory(TwilioPIQEConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
    };

    @Override
    public String getName() {
        return "TwilioPIQE";
    }

    @Override
    public void initialize(final Bootstrap<TwilioPIQEConfiguration> bootstrap) {
        bootstrap.addBundle(hibernate);
    }

    @Override
    public void run(final TwilioPIQEConfiguration configuration,
                    final Environment environment) {

        TwilioServicesProvider twilioClient = new TwilioServicesProvider(
            configuration.getTwilioAccountSid(),
            configuration.getTwilioAuthToken(),
            configuration.getTwilioMessagingServiceSid()
        );

        final AppointmentDAO appointmentDAO = new AppointmentDAO(hibernate.getSessionFactory());
        final PersonOrEntityDAO dao = new PersonOrEntityDAO(hibernate.getSessionFactory());
        final AlertQueueDAO aqDao = new AlertQueueDAO(hibernate.getSessionFactory());
        final AlertQueueMessageDAO aqmDao = new AlertQueueMessageDAO(hibernate.getSessionFactory());
        final CustomerTransactionDAO ctDao = new CustomerTransactionDAO(hibernate.getSessionFactory());

        final FilterRegistration.Dynamic cors =
            environment.servlets().addFilter("CORS", CrossOriginFilter.class);

        // Configure CORS parameters
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "http://localhost:3000");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin,Authorization");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "OPTIONS,GET,PUT,POST,DELETE,HEAD");
        cors.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true");

        environment.servlets()
            .addFilter("CORSFilter", new CORSFilter())
            .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");

        // JWT utility — shared between OAuthResource, JwtAuthenticator, and CreditCheckFilter
        final JwtUtil jwtUtil = new JwtUtil(configuration.getJwtSecret());

        environment.servlets()
            .addFilter("CreditCheckFilter", new CreditCheckFilter(
                configuration.getCostPerMessage(), hibernate.getSessionFactory(), jwtUtil))
            .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/api/queue/*");

        PersonOrEntityService personOrEntityService = new PersonOrEntityService(dao);

        NotificationsResource resource = new NotificationsResource(twilioClient, dao);
        InputResource inputResource = new InputResource(aqDao, personOrEntityService);
        PersonEntityResource personEntityResource = new PersonEntityResource(dao);
        MessageResultProcessor messageResultProcessor = new BasicMessageResultProcessor(ctDao, configuration.isDevMode(), hibernate.getSessionFactory());
        QueueResource queueResource = new QueueResource(twilioClient, aqDao, aqmDao, messageResultProcessor, configuration.getCostPerMessage(), configuration.getLowCreditThreshold());

        // Chained authentication: Basic Auth (username/password) + Bearer JWT (OAuth)
        environment.jersey().register(new AuthDynamicFeature(
            new ChainedAuthFilter<>(List.of(
                new BasicCredentialAuthFilter.Builder<User>()
                    .setAuthenticator(new SimpleAuthenticator(hibernate.getSessionFactory()))
                    .setAuthorizer(new SimpleAuthorizer())
                    .setRealm("PIQE")
                    .buildAuthFilter(),
                new OAuthCredentialAuthFilter.Builder<User>()
                    .setAuthenticator(new JwtAuthenticator(jwtUtil))
                    .setAuthorizer(new SimpleAuthorizer())
                    .setPrefix("Bearer")
                    .buildAuthFilter()
            ))));
        environment.jersey().register(RolesAllowedDynamicFeature.class);
        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(User.class));

        MediaResource mediaResource = new MediaResource(configuration.getMediaStoragePath(), configuration.getMediaBaseUrl());
        MmsResource mmsResource = new MmsResource(twilioClient, dao, messageResultProcessor, configuration.getMediaBaseUrl(), configuration.getCostPerMms());

        environment.jersey().register(resource);
        environment.jersey().register(MultiPartFeature.class);
        environment.jersey().register(mediaResource);
        environment.jersey().register(mmsResource);
        environment.jersey().register(queueResource);
        environment.jersey().register(inputResource);
        environment.jersey().register(personEntityResource);
        environment.jersey().register(new AuthResource());
        environment.jersey().register(new TransactionResource(ctDao));
        environment.jersey().register(new CreditResource(hibernate.getSessionFactory()));
        environment.jersey().register(new SmsWebhookResource(hibernate.getSessionFactory()));
        environment.jersey().register(new OAuthResource(configuration, hibernate.getSessionFactory(), jwtUtil));
        environment.jersey().register(new AppointmentResource(appointmentDAO));
    }
}
