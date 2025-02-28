import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class UnigramWordPredictorTest {
// Wave 4
    /**
     * Tests the train method by checking that the generated neighbor map matches the expected map.
     * 
     * The training data simulates the tokens:
     * "the cat sat. the cat slept. the dog barked."
     * 
     * This results in the following sequences:
     * - "the" is followed by "cat", then "cat", and finally "dog".
     * - "cat" is followed by "sat" and then "slept".
     * - "barked", "sat", and "slept" are both followed by a period (".").
     * - "dog" is followed by "barked".
     * -  is followed by a period (".").
     * - "." is followed by the word "the" twice.
     * 
     * The expected neighbor map is checked to see if it matches this pattern.
     * The test does not care about the order of the map or the lists.
     */
    @Test
    void testTrainAndGetNeighborMap() {
        // Simulated training text with tokenized words
        FakeTokenizer fakeTokenizer = new FakeTokenizer(
            List.of("the", "cat", "sat", ".", "the", "cat", "slept", ".", "the", "dog", "barked", ".")
        );

        UnigramWordPredictor predictor = new UnigramWordPredictor(fakeTokenizer);
        predictor.train(null); // Scanner input is ignored by FakeTokenizer

        Map<String, List<String>> neighborMap = predictor.getNeighborMap();

        for (List<String> values : neighborMap.values()) {
            values.sort(null);
        }
        // Pre-sorted expected map

        Map<String, List<String>> expectedMap = Map.of(
            "the", List.of("cat", "cat", "dog"),
            "cat", List.of("sat", "slept"),
            ".", List.of("the", "the"),
            "dog", List.of("barked"),
            "sat", List.of("."),
            "slept", List.of("."),
            "barked", List.of(".")
        );

        assertEquals(expectedMap, neighborMap);
    }

    // Wave 5
    /**
     * Tests the predictNextWord method using a different example to verify that the correct word
     * is predicted based on the training data.
     * 
     * The training data simulates the tokens:
     * "the quick brown fox. a quick red fox. the slow green turtle."
     * 
     * This results in the following patterns:
     * - "the" is followed by "quick" and "slow".
     * - "a" is followed by "quick".
     * - "quick" is followed by "brown" and "red".
     * - "fox" is followed by a period "." twice.
     * - "slow" is followed by "green".
     * - "green" is followed by "turtle".
     * - "turtle" is followed by a period ".".
     * - A period "." is followed by "the" twice and "a" once.
     * 
     * The test verifies that the predictions for various words are consistent with these patterns.
     */

    @Test
    void testPredictNextWord() {
                // Use a fake tokenizer with a new set of predefined tokens

        FakeTokenizer fakeTokenizer = new FakeTokenizer(
            List.of("the", "quick", "brown", "fox", ".", "a", "quick", "red", "fox", ".", "the", "slow", "green", "turtle", ".")
        );

        UnigramWordPredictor predictor = new UnigramWordPredictor(fakeTokenizer);
        predictor.train(null);

        assertTrue(List.of("quick", "slow").contains(predictor.predictNextWord(List.of("the"))));
        assertEquals("quick", predictor.predictNextWord(List.of("a")));
        assertTrue(List.of("brown", "red").contains(predictor.predictNextWord(List.of("quick"))));
        assertEquals(".", predictor.predictNextWord(List.of("fox")));
        assertEquals("green", predictor.predictNextWord(List.of("slow")));
        assertEquals(".", predictor.predictNextWord(List.of("turtle")));
        assertTrue(List.of("the", "a").contains(predictor.predictNextWord(List.of("."))));
    }

    // Wave 5
    /**
     * Tests the predictNextWord method probabilistically by performing multiple trials to check if
     * the predicted words appear with the expected frequencies.
     * 
     * The training data simulates the tokens:
     * "the cat sat. the cat slept. the dog barked."
     * 
     * Expected probabilities based on the training data:
     * - "the" is followed by "cat" twice and "dog" once, so "cat" should appear 2/3 of the time,
     *   and "dog" should appear 1/3 of the time.
     * - "cat" is followed by "sat" and "slept", so each should appear 50% of the time.
     * - "dog" is always followed by "barked".
     * - Each period is always followed by "the".
     * 
     * The test runs multiple trials to estimate these probabilities and compares them to the
     * expected values with some tolerance for variation.
     */

    @Test
    void testPredictNextWordProbabilistically() {
        FakeTokenizer fakeTokenizer = new FakeTokenizer(
            List.of("the", "cat", "sat", ".", "the", "cat", "slept", ".", "the", "dog", "barked", ".")
        );

        UnigramWordPredictor predictor = new UnigramWordPredictor(fakeTokenizer);
        predictor.train(null);

        // Number of trials for probability check
        int trials = 10000;
        double tolerance = 0.05; 

        Map<String, Map<String, Double>> expectedProbabilities = Map.of(
            "the", Map.of("cat", 2.0 / 3.0, "dog", 1.0 / 3.0),
            "cat", Map.of("sat", 0.5, "slept", 0.5),
            "dog", Map.of("barked", 1.0),
            ".", Map.of("the", 1.0)
        );

        for (String word : expectedProbabilities.keySet()) {
            Map<String, Integer> counts = new HashMap<>();
            Map<String, Double> expected = expectedProbabilities.get(word);

            expected.keySet().forEach(nextWord -> counts.put(nextWord, 0));

            for (int i = 0; i < trials; i++) {
                String predictedWord = predictor.predictNextWord(List.of(word));
                counts.put(predictedWord, counts.getOrDefault(predictedWord, 0) + 1);
            }

            for (String nextWord : expected.keySet()) {
                double observedFrequency = counts.get(nextWord) / (double) trials;
                double expectedFrequency = expected.get(nextWord);
                assertTrue(Math.abs(observedFrequency - expectedFrequency) < tolerance,
                        "Frequency mismatch for '" + word + "': observed " + observedFrequency + ", expected " + expectedFrequency);
            }
        }
    }
}
