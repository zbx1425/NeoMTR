package mtr.sound;

import mtr.MTR;
import net.minecraft.sounds.SoundEvent;

public interface SoundEvents {

	SoundEvent TICKET_BARRIER = SoundEvent.createVariableRangeEvent(MTR.id("ticket_barrier"));
	SoundEvent TICKET_BARRIER_CONCESSIONARY = SoundEvent.createVariableRangeEvent(MTR.id("ticket_barrier_concessionary"));
	SoundEvent TICKET_PROCESSOR_ENTRY = SoundEvent.createVariableRangeEvent(MTR.id("ticket_processor_entry"));
	SoundEvent TICKET_PROCESSOR_ENTRY_CONCESSIONARY = SoundEvent.createVariableRangeEvent(MTR.id("ticket_processor_entry_concessionary"));
	SoundEvent TICKET_PROCESSOR_EXIT = SoundEvent.createVariableRangeEvent(MTR.id("ticket_processor_exit"));
	SoundEvent TICKET_PROCESSOR_EXIT_CONCESSIONARY = SoundEvent.createVariableRangeEvent(MTR.id("ticket_processor_exit_concessionary"));
	SoundEvent TICKET_PROCESSOR_FAIL = SoundEvent.createVariableRangeEvent(MTR.id("ticket_processor_fail"));
}
