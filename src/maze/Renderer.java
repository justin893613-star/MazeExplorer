package maze;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 負責把迷宮「畫」到終端機上（純文字介面）。
 * <p>
 * 使用 ANSI 跳脫碼上色；若終端機不支援顏色（例如舊版 Windows cmd），
 * 可在主選單的「設定」關閉顏色，改用純符號顯示。
 */
public class Renderer {

    // ===== ANSI 顏色碼 =====
    static final String RESET = "\u001B[0m";
    static final String GRAY = "\u001B[90m";
    static final String GREEN = "\u001B[92m";
    static final String RED = "\u001B[91m";
    static final String YELLOW = "\u001B[93m";
    static final String CYAN = "\u001B[96m";
    static final String MAGENTA = "\u001B[95m";
    static final String BOLD = "\u001B[1m";

    /** 是否使用顏色（可在設定中切換）。 */
    public static boolean useColor = true;

    /** 依設定決定是否加上顏色。 */
    public static String color(String code, String text) {
        return useColor ? code + text + RESET : text;
    }

    /**
     * 畫出迷宮目前的狀態。
     *
     * @param maze    迷宮
     * @param visited 已探索的格子（顯示為 ·）
     * @param current 目前正在處理的格子（顯示為 @）；可為 null
     * @param path    最終路徑（顯示為 ●）；可為 null
     * @return 一整張畫面的字串
     */
    public static String draw(Maze maze, Set<Point> visited, Point current, List<Point> path) {
        Set<Point> pathSet = path == null ? Set.of() : new HashSet<>(path);
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < maze.rows(); r++) {
            for (int c = 0; c < maze.cols(); c++) {
                Point p = new Point(r, c);
                char ch = maze.cell(r, c);
                // 顯示優先順序：起終點 > 目前位置 > 路徑 > 已探索 > 牆/通道
                if (ch == Maze.START)               sb.append(color(BOLD + GREEN, "S "));
                else if (ch == Maze.END)            sb.append(color(BOLD + RED, "E "));
                else if (p.equals(current))         sb.append(color(BOLD + MAGENTA, "@ "));
                else if (pathSet.contains(p))       sb.append(color(YELLOW, "● "));
                else if (visited.contains(p))       sb.append(color(CYAN, "· "));
                else if (ch == Maze.WALL)           sb.append(color(GRAY, "██"));
                else                                sb.append("  ");
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /** 圖例說明，放在迷宮下方。 */
    public static String legend() {
        return color(BOLD + GREEN, "S") + " 起點  "
                + color(BOLD + RED, "E") + " 終點  "
                + color(BOLD + MAGENTA, "@") + " 目前位置  "
                + color(CYAN, "·") + " 已探索  "
                + color(YELLOW, "●") + " 最終路徑  "
                + color(GRAY, "██") + " 牆";
    }

    /** 清除畫面並把游標移回左上角（動畫用；IDE 主控台可能不支援）。 */
    public static void clearScreen() {
        System.out.print("\u001B[H\u001B[2J");
        System.out.flush();
    }
}
