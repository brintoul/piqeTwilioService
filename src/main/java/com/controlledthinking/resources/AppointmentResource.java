package com.controlledthinking.resources;

import com.controlledthinking.auth.User;
import com.controlledthinking.dao.AppointmentDAO;
import com.controlledthinking.db.Appointment;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.List;

@Path("/appointments")
public class AppointmentResource {

    private final AppointmentDAO appointmentDAO;

    public AppointmentResource(AppointmentDAO appointmentDAO) {
        this.appointmentDAO = appointmentDAO;
    }

    @POST
    @UnitOfWork
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAppointment(@Auth User user, @NotNull @Valid Appointment appointment) {
        appointment.setCustomer(appointmentDAO.getCustomerReference(user.getCustomerId()));
        Appointment created = appointmentDAO.create(appointment);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @Path("/{id}")
    @PUT
    @UnitOfWork
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateAppointment(@Auth User user,
                                      @PathParam("id") String id,
                                      @NotNull @Valid Appointment updated) {
        return appointmentDAO.findById(id)
                .map(existing -> {
                    if (!existing.getCustomer().getCustomerId().equals(user.getCustomerId())) {
                        return Response.status(Response.Status.FORBIDDEN).build();
                    }
                    existing.setFirstName(updated.getFirstName());
                    existing.setLastName(updated.getLastName());
                    existing.setPhoneNumber(updated.getPhoneNumber());
                    existing.setAppointmentTime(updated.getAppointmentTime());
                    return Response.ok("Updated").build();
                })
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @Path("/{id}")
    @DELETE
    @UnitOfWork
    public Response deleteAppointment(@Auth User user, @PathParam("id") String id) {
        return appointmentDAO.findById(id)
                .map(existing -> {
                    if (!existing.getCustomer().getCustomerId().equals(user.getCustomerId())) {
                        return Response.status(Response.Status.FORBIDDEN).build();
                    }
                    appointmentDAO.delete(existing);
                    return Response.noContent().build();
                })
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @UnitOfWork
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAppointments(@Auth User user,
                                    @QueryParam("from") String from,
                                    @QueryParam("to") String to) {
        if (from == null || to == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Query parameters 'from' and 'to' are required (ISO-8601 format)")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
        try {
            LocalDateTime fromDt = LocalDateTime.parse(from);
            LocalDateTime toDt = LocalDateTime.parse(to);
            List<Appointment> appointments = appointmentDAO.findByCustomerAndDateRange(user.getCustomerId(), fromDt, toDt);
            return Response.ok(appointments).build();
        } catch (java.time.format.DateTimeParseException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid date format. Use ISO-8601, e.g. 2026-04-01T00:00:00")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
    }
}
