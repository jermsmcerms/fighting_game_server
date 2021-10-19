package library;

public class SaveGameState {
    public byte[] obj_data;
    public long checksum;

    public SaveGameState(byte[] obj_data, long checksum) {
        this.obj_data = obj_data;
        this.checksum = checksum;
    }
}
