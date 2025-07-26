package sir_draco.survivalskills.god_questline;

import org.bukkit.World;
import org.bukkit.util.BoundingBox;

public record ProtectedArea(BoundingBox boundingBox, World world) {}
