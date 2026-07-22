package sir_draco.survivalskills.pipes;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record PipeRecord(UUID pipeUuid, UUID ownerUuid, PipeLocation location, PipeType type,
        Optional<UUID> senderUuid, List<UUID> receiverUuids, Set<PipeFilter> whitelist) {
    public PipeRecord {
        Objects.requireNonNull(pipeUuid);
        Objects.requireNonNull(ownerUuid);
        Objects.requireNonNull(location);
        Objects.requireNonNull(type);
        senderUuid = Objects.requireNonNull(senderUuid);
        receiverUuids = List.copyOf(receiverUuids);
        whitelist = Set.copyOf(whitelist);
        if (type == PipeType.SENDER && senderUuid.isPresent()) {
            throw new IllegalArgumentException("A sender cannot reference another sender");
        }
        if (type == PipeType.RECEIVER && !receiverUuids.isEmpty()) {
            throw new IllegalArgumentException("A receiver cannot own receiver links");
        }
    }
}
