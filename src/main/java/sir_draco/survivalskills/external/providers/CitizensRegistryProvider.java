package sir_draco.survivalskills.external.providers;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.MemoryNPCDataStore;
import net.citizensnpcs.api.npc.NPCRegistry;

public class CitizensRegistryProvider {
    private NPCRegistry registry;

    public CitizensRegistryProvider() {
        registry = CitizensAPI.createAnonymousNPCRegistry(new MemoryNPCDataStore());
    }
    
    public NPCRegistry getRegistry() {
        return registry;
    }
}
