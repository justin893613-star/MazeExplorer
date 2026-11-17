package maze;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

/**
 * 迷宮探險家 Maze Explorer —— 程式進入點（文字選單介面）。
 * <p>
 * 功能：
 * <ol>
 *   <li>DFS（堆疊）動畫求解</li>
 *   <li>BFS（佇列）動畫求解</li>
 *   <li>DFS vs BFS 並排對決，輸出比較表</li>
 *   <li>切換內建迷宮 / 產生隨機迷宮 / 從檔案載入迷宮</li>
 *   <li>設定動畫速度與顏色</li>
 * </ol>
 * 執行方式：在 IDE 中執行本類別的 main()，或於專案根目錄輸入
 * <pre>
 *   javac -encoding UTF-8 -d out src/maze/*.java
 *   java -cp out maze.Main
 * </pre>
 */
public class Main {

    /** 內建迷宮（直接寫在程式裡，確保不需要額外檔案也能執行）。 */
    private static final String[][] BUILT_IN = {
        {   // 0：入門小迷宮
            "###########",
            "#S..#.....#",
            "#.#.#.###.#",
            "#.#...#...#",
            "#.#####.#.#",
            "#.....#.#.#",
            "#####.#.#.#",
            "#.....#.#E#",
            "###########",
        },
        {   // 1：DFS 陷阱 —— 上方有一條很長的繞路，DFS 會先鑽進去
            "#################",
            "#S..............#",
            "#.#############.#",
            "#.#...........#.#",
            "#.#.#########.#.#",
            "#.#.#.......#.#.#",
            "#...#.#####.#...#",
            "###.#.#...#.###.#",
            "#...#...#.....#.#",
            "#.#############.#",
            "#..............E#",
            "#################",
        },
        {   // 2：開闊廣場 —— 觀察 BFS 的「水波擴散」
            "###############",
            "#S............#",
            "#.............#",
            "#....#####....#",
            "#....#...#....#",
            "#....#.E.#....#",
            "#....#...#....#",
            "#....##.##....#",
            "#.............#",
            "###############",
        },
    };
    private static final String[] BUILT_IN_NAMES = { "入門小迷宮", "DFS 陷阱", "開闊廣場" };

    private static final Scanner in = new Scanner(System.in, "UTF-8");
    private static Maze maze;
    private static int builtInIndex = 0;
    /** 動畫每一步的停頓毫秒數；0 代表不播動畫，直接顯示結果。 */
    private static int delayMs = 60;

    public static void main(String[] args) {
        maze = new Maze(BUILT_IN_NAMES[0], List.of(BUILT_IN[0]));
        printBanner();
        if (!isRealTerminal()) {
            // NetBeans / IntelliJ 等 IDE 的輸出視窗不支援「清除畫面」，
            // 動畫每一格都會往下印而洗版，所以預設改為直接顯示結果。
            delayMs = 0;
            System.out.println(Renderer.color(Renderer.YELLOW,
                    "偵測到 IDE 輸出視窗：動畫預設關閉（可在選項 7 開啟）。用系統終端機執行 run.bat / run.sh 可看完整動畫。"));
        }
        try {
            while (true) {
                showMenu();
                String choice = ask("請輸入選項編號：");
                switch (choice) {
                    case "1" -> runAnimated(Solver.Algorithm.DFS);
                    case "2" -> runAnimated(Solver.Algorithm.BFS);
                    case "3" -> compare();
                    case "4" -> nextBuiltIn();
                    case "5" -> randomMaze();
                    case "6" -> loadFromFile();
                    case "7" -> settings();
                    case "0" -> { System.out.println("掰掰，祝你永遠找得到出口！"); return; }
                    default -> System.out.println(Renderer.color(Renderer.RED, "沒有這個選項，請輸入 0~7 的數字喔。"));
                }
            }
        } catch (NoSuchElementException e) {
            // 輸入串流結束（例如用檔案重導向輸入時），安靜結束程式
            System.out.println("\n輸入結束，程式關閉。");
        }
    }

    // ======================== 畫面 ========================

    private static void printBanner() {
        System.out.println(Renderer.color(Renderer.BOLD + Renderer.CYAN,
                "╔══════════════════════════════════════════╗\n"
              + "║   迷宮探險家 Maze Explorer               ║\n"
              + "║   堆疊 DFS  vs  佇列 BFS  路徑求解對決   ║\n"
              + "╚══════════════════════════════════════════╝"));
    }

    private static void showMenu() {
        System.out.println();
        System.out.println(Renderer.color(Renderer.BOLD, "目前迷宮：") + maze.name()
                + "（" + maze.rows() + " 列 x " + maze.cols() + " 行）");
        System.out.print(Renderer.draw(maze, Set.of(), null, null));
        System.out.println("────────────────────────────────────────");
        System.out.println(" 1. 用 DFS 走迷宮（堆疊，一路走到底）");
        System.out.println(" 2. 用 BFS 走迷宮（佇列，一圈圈擴散）");
        System.out.println(" 3. DFS vs BFS 對決（並排比較）");
        System.out.println(" 4. 換下一個內建迷宮");
        System.out.println(" 5. 產生隨機迷宮");
        System.out.println(" 6. 從檔案載入迷宮");
        System.out.println(" 7. 設定（動畫速度：" + (delayMs == 0 ? "關閉" : delayMs + "ms")
                + "，顏色：" + (Renderer.useColor ? "開" : "關") + "）");
        System.out.println(" 0. 離開");
        System.out.println("────────────────────────────────────────");
    }

    /** 顯示提示文字並讀取一行輸入（去頭尾空白）。 */
    private static String ask(String prompt) {
        System.out.print(prompt);
        return in.nextLine().trim();
    }

    /**
     * 讀取整數；輸入空白時回傳預設值，輸入錯誤會請使用者重新輸入。
     */
    private static int askInt(String prompt, int min, int max, int defaultValue) {
        while (true) {
            String s = ask(prompt + "（" + min + "~" + max + "，直接按 Enter = " + defaultValue + "）：");
            if (s.isEmpty()) return defaultValue;
            try {
                int v = Integer.parseInt(s);
                if (v >= min && v <= max) return v;
            } catch (NumberFormatException ignored) {
                // 不是數字，落到下面的提示
            }
            System.out.println(Renderer.color(Renderer.RED, "請輸入 " + min + " 到 " + max + " 之間的整數。"));
        }
    }

    // ======================== 功能 ========================

    /**
     * 選項 1、2：先算出完整結果，再依探索順序一格一格重播成動畫。
     */
    private static void runAnimated(Solver.Algorithm algo) {
        Solver.Result res = Solver.solve(maze, algo);
        Set<Point> shown = new HashSet<>();

        if (delayMs > 0) {
            for (int i = 0; i < res.visitOrder.size(); i++) {
                Point p = res.visitOrder.get(i);
                shown.add(p);
                Renderer.clearScreen();
                System.out.println(Renderer.color(Renderer.BOLD, algo.label));
                System.out.print(Renderer.draw(maze, shown, p, null));
                System.out.printf("第 %d 步，目前位置 %s，%s 中還有 %d 格等待%n",
                        i + 1, p, algo == Solver.Algorithm.DFS ? "堆疊" : "佇列", res.frontierSizes.get(i));
                sleep(delayMs);
            }
            Renderer.clearScreen();
        } else {
            shown.addAll(res.visitOrder);
        }

        // 最後畫出完整結果（含黃色路徑）
        System.out.println(Renderer.color(Renderer.BOLD, algo.label + " ── 結果"));
        System.out.print(Renderer.draw(maze, shown, null, res.path));
        System.out.println(Renderer.legend());
        if (res.found()) {
            System.out.printf("找到出口！探索了 %d 格，路徑長度 %d 步。%n", res.visitOrder.size(), res.pathSteps());
            System.out.println("路徑：" + summarizePath(res.path));
        } else {
            System.out.println(Renderer.color(Renderer.RED, "這個迷宮沒有通往出口的路！"));
        }
        ask("\n按 Enter 回到選單…");
    }

    /**
     * 選項 3：DFS 與 BFS 跑同一個迷宮，左右並排顯示，並列出比較表。
     */
    private static void compare() {
        Solver.Result dfs = Solver.dfs(maze);
        Solver.Result bfs = Solver.bfs(maze);

        String[] left = Renderer.draw(maze, new HashSet<>(dfs.visitOrder), null, dfs.path).split("\n");
        String[] right = Renderer.draw(maze, new HashSet<>(bfs.visitOrder), null, bfs.path).split("\n");
        String gap = "    ";
        int width = maze.cols() * 2;

        System.out.println();
        System.out.println(pad("【DFS 堆疊】", width) + gap + "【BFS 佇列】");
        for (int i = 0; i < left.length; i++) {
            System.out.println(left[i] + gap + right[i]);
        }
        System.out.println(Renderer.legend());

        // ===== 比較表 =====
        int shortest = bfs.pathSteps();   // BFS 的結果就是最短路徑
        System.out.println();
        System.out.println("┌──────────────────┬────────────┬────────────┐");
        System.out.println(tableRow("項目", "DFS 堆疊", "BFS 佇列"));
        System.out.println("├──────────────────┼────────────┼────────────┤");
        System.out.println(tableRow("探索格數", num(dfs.visitOrder.size()), num(bfs.visitOrder.size())));
        System.out.println(tableRow("路徑步數", num(dfs.pathSteps()), num(bfs.pathSteps())));
        System.out.println(tableRow("等待區最大量", num(dfs.maxFrontier()), num(bfs.maxFrontier())));
        System.out.println(tableRow("是否最短路徑",
                yesNo(dfs.found() && dfs.pathSteps() == shortest), yesNo(bfs.found())));
        System.out.println(tableRow("執行時間",
                String.format("%.3fms", dfs.nanos / 1e6), String.format("%.3fms", bfs.nanos / 1e6)));
        System.out.println("└──────────────────┴────────────┴────────────┘");

        // ===== 自動講評 =====
        System.out.println(Renderer.color(Renderer.BOLD, "\n講評："));
        if (!dfs.found()) {
            System.out.println("・兩種方法都找不到出口，這是一座死迷宮。");
        } else {
            if (dfs.pathSteps() > shortest) {
                System.out.printf("・DFS 繞了遠路，比最短路徑多走 %d 步，因為它「看到路就往深處鑽」。%n",
                        dfs.pathSteps() - shortest);
            } else {
                System.out.println("・這次 DFS 運氣不錯，剛好也走到最短路徑。");
            }
            if (dfs.visitOrder.size() < bfs.visitOrder.size()) {
                System.out.println("・DFS 探索的格子比較少：方向剛好猜對時，深度優先很省事。");
            } else {
                System.out.println("・BFS 探索的格子沒有比較多，而且保證找到最短路徑。");
            }
            if (bfs.maxFrontier() > dfs.maxFrontier()) {
                System.out.println("・BFS 的佇列比較「胖」（同時要記住一整圈的格子），這是取得最短路徑的代價。");
            }
        }
        ask("\n按 Enter 回到選單…");
    }

    /** 組出比較表的一列：第一欄 16 格寬、後兩欄各 10 格寬（中文字算 2 格）。 */
    private static String tableRow(String name, String a, String b) {
        return "│ " + pad(name, 16) + " │ " + pad(a, 10) + " │ " + pad(b, 10) + " │";
    }

    /** 數字轉字串；-1 代表「沒有路徑」，顯示為破折號。 */
    private static String num(int n) { return n < 0 ? "—" : String.valueOf(n); }

    private static String yesNo(boolean ok) { return ok ? "是 ✔" : "否 ✘"; }

    private static String pad(String s, int width) {
        // 中文字在終端機佔 2 格寬，計算「視覺寬度」後補空白，讓表格與標題對齊
        int visual = 0;
        for (char ch : s.toCharArray()) visual += ch > 0x2E80 ? 2 : 1;
        return s + " ".repeat(Math.max(0, width - visual));
    }

    /** 選項 4：輪流切換內建迷宮。 */
    private static void nextBuiltIn() {
        builtInIndex = (builtInIndex + 1) % BUILT_IN.length;
        maze = new Maze(BUILT_IN_NAMES[builtInIndex], List.of(BUILT_IN[builtInIndex]));
        System.out.println(Renderer.color(Renderer.GREEN, "已切換為：" + maze.name()));
    }

    /** 選項 5：輸入大小與種子，產生隨機迷宮。 */
    private static void randomMaze() {
        int rows = askInt("列數", 7, 41, 15);
        int cols = askInt("行數", 7, 61, 25);
        int seed = askInt("亂數種子（相同種子 = 相同迷宮）", 0, 99999, (int) (System.currentTimeMillis() % 100000));
        maze = Maze.random(rows, cols, seed);
        System.out.println(Renderer.color(Renderer.GREEN, "已產生：" + maze.name()));
    }

    /** 選項 6：讀入使用者自訂的迷宮文字檔。 */
    private static void loadFromFile() {
        System.out.println("檔案格式：# 牆、. 通道、S 起點、E 終點。範例檔放在 mazes/ 資料夾。");
        String file = ask("請輸入檔名（例如 mazes/spiral.txt）：");
        try {
            Path p = Path.of(file);
            if (!Files.exists(p)) {
                System.out.println(Renderer.color(Renderer.RED, "找不到檔案：" + p.toAbsolutePath()));
                return;
            }
            maze = Maze.fromFile(p);
            System.out.println(Renderer.color(Renderer.GREEN, "載入成功：" + maze.name()));
        } catch (Exception e) {
            System.out.println(Renderer.color(Renderer.RED, "載入失敗：" + e.getMessage()));
        }
    }

    /** 選項 7：調整動畫速度與顏色。 */
    private static void settings() {
        delayMs = askInt("動畫每步停頓毫秒數，0 = 不播動畫", 0, 1000, delayMs);
        String c = ask("要使用顏色嗎？(y/n，直接按 Enter 不變)：");
        if (c.equalsIgnoreCase("y")) Renderer.useColor = true;
        if (c.equalsIgnoreCase("n")) Renderer.useColor = false;
        System.out.println("設定已更新。");
    }

    // ======================== 工具 ========================

    /**
     * 判斷目前是否在「真正的終端機」中執行（支援清除畫面，可以播動畫）。
     * <ul>
     *   <li>System.console() 為 null：輸出被導向 IDE 視窗或檔案 → 不是終端機。</li>
     *   <li>Java 22 起 Console 新增 isTerminal()，用反射呼叫，舊版 JDK 也能編譯執行。</li>
     * </ul>
     */
    private static boolean isRealTerminal() {
        java.io.Console console = System.console();
        if (console == null) return false;
        try {
            java.lang.reflect.Method m = java.io.Console.class.getMethod("isTerminal");
            return (Boolean) m.invoke(console);
        } catch (NoSuchMethodException e) {
            return true;    // Java 21 以前：console 不為 null 就代表是終端機
        } catch (ReflectiveOperationException e) {
            return true;
        }
    }

    /** 路徑太長時只顯示頭尾，避免洗版。 */
    private static String summarizePath(List<Point> path) {
        if (path.size() <= 12) return path.toString();
        return path.subList(0, 5) + " … " + path.subList(path.size() - 5, path.size());
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
