package eu.hiddenite.casino.machine.fall;

import java.util.*;

public class FallMachineResult {
    private List<List<Integer>> rows;
    private int inputPrice;
    private ArrayList<Integer> scores = new ArrayList<>();
    private int finalScore;

    public FallMachineResult(List<List<Integer>> rows, int inputPrice) {
        this.rows = rows;
        this.inputPrice = inputPrice;
        setScores(rows);
    }

    public FallMachineResult(int inputPrice) {
        this.rows = generateRows();
        this.inputPrice = inputPrice;
        setScores(rows);
    }

    private static List<List<Integer>> generateRows() {
        List<List<Integer>> rows = new ArrayList<>();
        for (var i = 0; i < 3; i += 1) {
            List<Integer> row = new ArrayList<>();
            for (var j = 0; j < 3; j += 1) {
                var score = (Math.random() * 5 % 5) + 1;
                row.add((int)score);
            }
            rows.add(row);
        }
        return rows;
    }

    public List<List<Integer>> getRows() {
        return rows;
    }

    private void setScores(List<List<Integer>> rows) {
        assert (rows.size() == 3);
        for (var row : rows) {
            assert (row.size() == 3);
            if (row.get(0).equals(row.get(1)) && row.get(0).equals(row.get(2))) {
                scores.add(row.get(0));
                finalScore += row.get(0);
            }
        }
        var first = rows.get(0).get(0);
        if (first.equals(rows.get(1).get(1)) && first.equals(rows.get(2).get(2))) {
            scores.add(first);
            finalScore += first;
        }
        first = rows.get(2).get(0);
        if (first.equals(rows.get(1).get(1)) && first.equals(rows.get(0).get(2))) {
            scores.add(first);
            finalScore += first;
        }
    }

    public int getFinalScore() {
        return finalScore;
    }

    public int getGain() {
        return finalScore * inputPrice;
    }

    public List<Integer> getScores() {
        return scores;
    }

    public String getMessage() {
        final String[] message = {""};
        var scoresCount = new HashMap<Integer, Integer>();
        for (var score : scores) {
            var value = scoresCount.get(score);
            scoresCount.put(score, value == null ? 1 : value + 1);
        }
        var tmp = new Object() {
            boolean first = true;
        };
        scoresCount.forEach((score, count) -> {
            if (tmp.first) {
                tmp.first = false;
            } else {
                message[0] += " ";
            }
            message[0] += String.format("%dx%d", score * inputPrice, count);
        });
        return message[0];
    }
}