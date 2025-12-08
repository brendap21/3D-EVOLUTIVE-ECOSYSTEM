package entities;

// Reexport the main.Renderable interface to avoid duplicate definitions.
// Any code importing entities.Renderable will now refer to the single canonical interface.
public interface Renderable extends main.Renderable {}
