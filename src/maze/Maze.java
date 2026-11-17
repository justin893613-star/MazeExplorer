package maze;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * 迷宮資料模型。
 * <p>
 * 以二維字元陣列儲存地圖，符號定義：
 * <ul>
 *   <li>{@code #} 牆壁</li>
 *   <li>{@code .} 通道（空白也視為通道）</li>
 *   <li>{@code S} 起點（Start）</li>
 *   <li>{@code E} 終點（End / Exit）</li>
 * </ul>
 * 迷宮可以從文字檔讀入，或用「堆疊 + 隨機回溯法」自動產生。
 */
public class Maze {

    public static final char WALL = '#';
    public static final char OPEN = '.';
    public static final char START = 'S';
    public static final char END = 'E';

    /** 地圖本體：grid[列][行] */
    private final char[][] grid;
    private final String name;
    private Point start;
    private Point end;

    /**
     * 由多行文字建立迷宮。
     *
     * @param name  迷宮名稱（顯示用）
     * @param lines 每一行代表迷宮的一列
     * @throws IllegalArgumentException 找不到 S 或 E、或內容為空時拋出
     */
    public Maze(String name, List<String> lines) {
        this.name = name;
        // 去掉空白行，並以最長的一行作為寬度（不足補牆，避免陣列越界）
        List<String> rows = new ArrayList<>();
        for (String line : lines) {
            if (!line.isBlank()) rows.add(line.stripTrailing());
        }
        if (rows.isEmpty()) throw new IllegalArgumentException("迷宮內容是空的");

        int width = 0;
        for (String row : rows) width = Math.max(width, row.length());

        grid = new char[rows.size()][width];
        for (int r = 0; r < rows.size(); r++) {
            for (int c = 0; c < width; c++) {
                char ch = c < rows.get(r).length() ? rows.get(r).charAt(c) : WALL;
                if (ch == ' ') ch = OPEN;           // 空白當作通道
                if (ch == START) start = new Point(r, c);
                if (ch == END) end = new Point(r, c);
                grid[r][c] = ch;
            }
        }
        if (start == null) throw new IllegalArgumentException("迷宮缺少起點 S");
        if (end == null) throw new IllegalArgumentException("迷宮缺少終點 E");
    }

    /**
     * 從文字檔讀入迷宮（UTF-8）。
     *
     * @param file 迷宮檔案路徑，例如 mazes/classic.txt
     */
    public static Maze fromFile(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        return new Maze(file.getFileName().toString(), lines);
    }

    /**
     * 用「隨機回溯法 (Recursive Backtracker)」產生一個隨機迷宮。
     * <p>
     * 這裡刻意用 <b>堆疊 (Stack)</b> 取代遞迴：
     * <ol>
     *   <li>把起點推入堆疊。</li>
     *   <li>看堆疊頂端的格子，隨機挑一個「還沒挖過」的鄰居（相隔 2 格）。</li>
     *   <li>有的話：打通中間的牆，把鄰居推入堆疊（往前挖）。</li>
     *   <li>沒有的話：pop 彈出（死路，倒退回上一格）。</li>
     *   <li>堆疊清空時，整個迷宮就挖好了。</li>
     * </ol>
     * 最後再隨機打掉一些牆，製造多條路線，讓 DFS 與 BFS 的差異更明顯。
     *
     * @param rows  列數（自動調整為奇數，最小 7）
     * @param cols  行數（自動調整為奇數，最小 7）
     * @param seed  亂數種子（相同種子會產生相同迷宮，方便重現展示）
     */
    public static Maze random(int rows, int cols, long seed) {
        rows = Math.max(7, rows | 1);   // | 1 讓數字變成奇數
        cols = Math.max(7, cols | 1);
        Random rnd = new Random(seed);

        char[][] g = new char[rows][cols];
        for (char[] row : g) java.util.Arrays.fill(row, WALL);

        // ===== 堆疊：記錄「挖掘路徑」，走不下去就 pop 回頭 =====
        Deque<Point> stack = new ArrayDeque<>();
        Point first = new Point(1, 1);
        g[1][1] = OPEN;
        stack.push(first);

        while (!stack.isEmpty()) {
            Point cur = stack.peek();
            List<int[]> dirs = new ArrayList<>(List.of(Point.DIRS));
            Collections.shuffle(dirs, rnd);       // 打亂方向，每次挖出不同迷宮

            boolean dug = false;
            for (int[] d : dirs) {
                int nr = cur.r() + d[0] * 2, nc = cur.c() + d[1] * 2;
                if (nr > 0 && nr < rows - 1 && nc > 0 && nc < cols - 1 && g[nr][nc] == WALL) {
                    g[cur.r() + d[0]][cur.c() + d[1]] = OPEN;   // 打通中間的牆
                    g[nr][nc] = OPEN;
                    stack.push(new Point(nr, nc));             // 往前挖
                    dug = true;
                    break;
                }
            }
            if (!dug) stack.pop();                              // 死路 → 回頭
        }

        // 額外打掉約 8% 的內牆，形成「環路」，讓最短路徑不只一條
        int extra = rows * cols / 12;
        for (int i = 0; i < extra; i++) {
            int r = 1 + rnd.nextInt(rows - 2), c = 1 + rnd.nextInt(cols - 2);
            g[r][c] = OPEN;
        }

        g[1][1] = START;
        g[rows - 2][cols - 2] = END;

        List<String> lines = new ArrayList<>();
        for (char[] row : g) lines.add(new String(row));
        return new Maze("隨機迷宮 " + rows + "x" + cols + " (seed=" + seed + ")", lines);
    }

    /** 判斷某座標是否在地圖內且不是牆（可以走）。 */
    public boolean isWalkable(Point p) {
        return p.r() >= 0 && p.r() < grid.length
                && p.c() >= 0 && p.c() < grid[0].length
                && grid[p.r()][p.c()] != WALL;
    }

    public char cell(int r, int c) { return grid[r][c]; }
    public int rows() { return grid.length; }
    public int cols() { return grid[0].length; }
    public Point start() { return start; }
    public Point end() { return end; }
    public String name() { return name; }
}
