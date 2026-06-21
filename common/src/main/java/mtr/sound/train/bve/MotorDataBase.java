package mtr.sound.train.bve;

public abstract class MotorDataBase {

	public abstract int getSoundCount();

	public abstract float getPitch(int index, float speed, float accel);

	public abstract float getVolume(int index, float speed, float accel);

}
