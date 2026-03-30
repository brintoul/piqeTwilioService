package com.controlledthinking.resources;

import com.controlledthinking.auth.User;
import io.dropwizard.auth.Auth;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/media")
public class MediaResource {

    private static final Logger logger = LoggerFactory.getLogger(MediaResource.class);

    private static final Map<String, String> ALLOWED_EXTENSIONS = Map.of(
        "jpg",  "image/jpeg",
        "jpeg", "image/jpeg",
        "png",  "image/png",
        "gif",  "image/gif"
    );

    private final String mediaStoragePath;
    private final String mediaBaseUrl;

    public MediaResource(String mediaStoragePath, String mediaBaseUrl) {
        this.mediaStoragePath = mediaStoragePath;
        this.mediaBaseUrl = mediaBaseUrl;
        new File(mediaStoragePath).mkdirs();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadMedia(@Auth User user,
                                @FormDataParam("file") InputStream uploadedInputStream,
                                @FormDataParam("file") FormDataContentDisposition fileDetail) {
        if (uploadedInputStream == null || fileDetail == null) {
            return Response.status(400).entity("No file provided").type(MediaType.TEXT_PLAIN).build();
        }

        String originalFilename = fileDetail.getFileName();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return Response.status(400).entity("File must have an extension").type(MediaType.TEXT_PLAIN).build();
        }

        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.containsKey(ext)) {
            return Response.status(400)
                    .entity("Unsupported file type. Allowed: jpg, jpeg, png, gif")
                    .type(MediaType.TEXT_PLAIN).build();
        }

        String mediaId = UUID.randomUUID() + "." + ext;
        java.nio.file.Path destination = Paths.get(mediaStoragePath, mediaId);

        try {
            Files.copy(uploadedInputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Failed to store uploaded media: {}", e.getMessage());
            return Response.serverError().entity("Failed to store file").type(MediaType.TEXT_PLAIN).build();
        }

        String mediaUrl = mediaBaseUrl + "/" + mediaId;
        logger.info("Media uploaded by {}: {}", user.getName(), mediaUrl);

        return Response.ok(Map.of("mediaId", mediaId, "url", mediaUrl)).build();
    }

    @GET
    @Path("/{mediaId: [^/]+}")
    public Response serveMedia(@PathParam("mediaId") String mediaId) {
        // Prevent path traversal
        if (mediaId.contains("..") || mediaId.contains("/")) {
            return Response.status(400).build();
        }

        File file = Paths.get(mediaStoragePath, mediaId).toFile();
        if (!file.exists()) {
            return Response.status(404).build();
        }

        String ext = mediaId.contains(".")
                ? mediaId.substring(mediaId.lastIndexOf('.') + 1).toLowerCase()
                : "";
        String contentType = ALLOWED_EXTENSIONS.getOrDefault(ext, "application/octet-stream");

        return Response.ok(file, contentType).build();
    }
}
