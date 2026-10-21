package ua.kpi.pastushenko.cosmocats.application.exception;

public final class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String resource, long id) {
        super(resource + " with id " + id + " was not found");
    }
}
