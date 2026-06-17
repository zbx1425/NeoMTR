package mtr;

import mtr.mappings.RegistryUtilities;
import net.minecraft.sounds.SoundEvent;

public interface SoundEvents {

	SoundEvent TICKET_BARRIER = RegistryUtilities.createSoundEvent(MTR.id("ticket_barrier"));
	SoundEvent TICKET_BARRIER_CONCESSIONARY = RegistryUtilities.createSoundEvent(MTR.id("ticket_barrier_concessionary"));
	SoundEvent TICKET_PROCESSOR_ENTRY = RegistryUtilities.createSoundEvent(MTR.id("ticket_processor_entry"));
	SoundEvent TICKET_PROCESSOR_ENTRY_CONCESSIONARY = RegistryUtilities.createSoundEvent(MTR.id("ticket_processor_entry_concessionary"));
	SoundEvent TICKET_PROCESSOR_EXIT = RegistryUtilities.createSoundEvent(MTR.id("ticket_processor_exit"));
	SoundEvent TICKET_PROCESSOR_EXIT_CONCESSIONARY = RegistryUtilities.createSoundEvent(MTR.id("ticket_processor_exit_concessionary"));
	SoundEvent TICKET_PROCESSOR_FAIL = RegistryUtilities.createSoundEvent(MTR.id("ticket_processor_fail"));
}
