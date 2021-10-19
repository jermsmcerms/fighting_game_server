package library;

public interface IPollSink {
    public default boolean onHandlePoll(Object o) { return true; }
    public default boolean onMsgPoll(Object o) { return true; }
    public default boolean onPeriodicPoll(Object o, int i) { return true; }
    public default boolean onLoopPoll(Object o) { return true; }
}
