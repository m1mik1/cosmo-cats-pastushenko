package ua.kpi.pastushenko.cosmocats.domain.model;

public final class Category {

    private final Long id;
    private final String name;

    public Category(Long id, String name) {
        DomainChecks.optionalId(id);
        DomainChecks.name(name, 1);
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
