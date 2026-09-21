package board;

/** 순위표에 없는 선수. 이 파일은 그대로 둔다. */
public class NoSuchPlayerException extends RuntimeException {
    public NoSuchPlayerException(String player) { super("no such player: " + player); }
}
