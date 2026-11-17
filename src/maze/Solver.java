package maze;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * 迷宮求解器：同一個迷宮，用兩種策略來找路。
 *
 * <table border="1">
 *   <caption>本專案用到的資料結構</caption>
 *   <tr><th>結構</th><th>Java 類別</th><th>用途</th></tr>
 *   <tr><td>堆疊 Stack</td><td>ArrayDeque (push/pop)</td><td>DFS：後進先出，一路走到底再回頭</td></tr>
 *   <tr><td>佇列 Queue</td><td>LinkedList (offer/poll)</td><td>BFS：先進先出，一圈一圈往外擴散</td></tr>
 *   <tr><td>集合 Set</td><td>HashSet</td><td>記錄「已經走過」的格子，避免繞圈</td></tr>
 *   <tr><td>字典 Map</td><td>HashMap&lt;子, 父&gt;</td><td>記錄每一格是從哪一格走來，用來回推路徑</td></tr>
 *   <tr><td>清單 List</td><td>ArrayList</td><td>記錄探索順序（動畫用）與最後的路徑</td></tr>
 * </table>
 */
public class Solver {

    /** 演算法種類。 */
    public enum Algorithm {
        DFS("DFS 深度優先（堆疊 Stack）"),
        BFS("BFS 廣度優先（佇列 Queue）");

        public final String label;
        Algorithm(String label) { this.label = label; }
    }

    /**
     * 求解結果：除了路徑之外，也保存「過程」，讓畫面能一步一步重播。
     */
    public static class Result {
        public final Algorithm algorithm;
        /** 探索順序：第 i 步拜訪了哪一格。 */
        public final List<Point> visitOrder = new ArrayList<>();
        /** 每一步時，堆疊/佇列裡還有幾格在等待（觀察兩種結構的大小變化）。 */
        public final List<Integer> frontierSizes = new ArrayList<>();
        /** 從起點到終點的路徑；找不到時為空清單。 */
        public List<Point> path = new ArrayList<>();
        /** 執行時間（奈秒）。 */
        public long nanos;

        Result(Algorithm algorithm) { this.algorithm = algorithm; }

        public boolean found() { return !path.isEmpty(); }

        /** 路徑步數 = 格子數 - 1（起點不算一步）。 */
        public int pathSteps() { return found() ? path.size() - 1 : -1; }

        /** 等待區（堆疊/佇列）最多同時放了幾格。 */
        public int maxFrontier() {
            int max = 0;
            for (int n : frontierSizes) max = Math.max(max, n);
            return max;
        }
    }

    /** 依指定演算法求解。 */
    public static Result solve(Maze maze, Algorithm algo) {
        return algo == Algorithm.DFS ? dfs(maze) : bfs(maze);
    }

    /**
     * 深度優先搜尋 DFS —— 使用 <b>堆疊</b>。
     * <p>
     * 每次 pop 出「最新放進去」的格子，所以會沿著一條路一直往深處走，
     * 遇到死路時，堆疊自然會讓我們回到最近的岔路口（回溯）。
     * <br>特性：記憶體用量與路線深度有關，找到的<b>不一定是最短路徑</b>。
     */
    public static Result dfs(Maze maze) {
        Result res = new Result(Algorithm.DFS);
        long t0 = System.nanoTime();

        Deque<Point> stack = new ArrayDeque<>();     // 堆疊：後進先出 LIFO
        Set<Point> visited = new HashSet<>();        // 集合：走過的格子
        Map<Point, Point> parent = new HashMap<>();  // 字典：子格 → 父格

        stack.push(maze.start());
        while (!stack.isEmpty()) {
            Point cur = stack.pop();                 // 取出最上面（最新）的格子
            if (visited.contains(cur)) continue;     // 同一格可能被推入多次，已走過就跳過
            visited.add(cur);
            res.visitOrder.add(cur);
            res.frontierSizes.add(stack.size());

            if (cur.equals(maze.end())) {            // 抵達終點
                res.path = buildPath(parent, maze.start(), cur);
                break;
            }
            // 反向推入四個方向，讓 pop 出來的順序剛好是「上、下、左、右」
            for (int i = Point.DIRS.length - 1; i >= 0; i--) {
                Point next = cur.move(Point.DIRS[i][0], Point.DIRS[i][1]);
                if (maze.isWalkable(next) && !visited.contains(next)) {
                    parent.put(next, cur);           // 最後推入者最先被 pop，所以覆寫是正確的
                    stack.push(next);
                }
            }
        }
        res.nanos = System.nanoTime() - t0;
        return res;
    }

    /**
     * 廣度優先搜尋 BFS —— 使用 <b>佇列</b>。
     * <p>
     * 每次 poll 出「最早放進去」的格子，所以會先走完距離 1 的格子、
     * 再走距離 2 的格子……像水波一樣一圈圈往外擴散。
     * <br>特性：第一次碰到終點時，走的一定是<b>最短路徑</b>（每步成本相同時）。
     */
    public static Result bfs(Maze maze) {
        Result res = new Result(Algorithm.BFS);
        long t0 = System.nanoTime();

        Queue<Point> queue = new LinkedList<>();     // 佇列：先進先出 FIFO
        Set<Point> visited = new HashSet<>();        // 集合：已排入佇列的格子
        Map<Point, Point> parent = new HashMap<>();  // 字典：子格 → 父格

        queue.offer(maze.start());
        visited.add(maze.start());                   // BFS 在「排隊時」就標記，避免重複排隊
        while (!queue.isEmpty()) {
            Point cur = queue.poll();                // 取出排在最前面（最早）的格子
            res.visitOrder.add(cur);
            res.frontierSizes.add(queue.size());

            if (cur.equals(maze.end())) {
                res.path = buildPath(parent, maze.start(), cur);
                break;
            }
            for (int[] d : Point.DIRS) {
                Point next = cur.move(d[0], d[1]);
                if (maze.isWalkable(next) && !visited.contains(next)) {
                    visited.add(next);
                    parent.put(next, cur);
                    queue.offer(next);               // 排到隊伍最後面
                }
            }
        }
        res.nanos = System.nanoTime() - t0;
        return res;
    }

    /**
     * 從終點沿著 parent 字典一路往回查，直到回到起點，再反轉成「起點 → 終點」。
     */
    private static List<Point> buildPath(Map<Point, Point> parent, Point start, Point end) {
        List<Point> path = new ArrayList<>();
        for (Point p = end; p != null; p = parent.get(p)) {
            path.add(p);
            if (p.equals(start)) break;
        }
        Collections.reverse(path);
        return path;
    }
}
