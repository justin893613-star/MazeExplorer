package maze;

/**
 * 迷宮中的一個座標 (列 row, 行 col)。
 * <p>
 * 使用 Java record：自動產生 equals() 與 hashCode()，
 * 因此可以直接放進 HashSet（記錄走過的格子）與 HashMap（記錄從哪一格走來）。
 *
 * @param r 列（由上往下，從 0 開始）
 * @param c 行（由左往右，從 0 開始）
 */
public record Point(int r, int c) {

    /** 上、下、左、右 四個方向的位移量（固定順序，讓結果可重現）。 */
    public static final int[][] DIRS = { {-1, 0}, {1, 0}, {0, -1}, {0, 1} };

    /** 回傳往某方向移動 (dr, dc) 之後的新座標。 */
    public Point move(int dr, int dc) {
        return new Point(r + dr, c + dc);
    }

    @Override
    public String toString() {
        return "(" + r + "," + c + ")";
    }
}
