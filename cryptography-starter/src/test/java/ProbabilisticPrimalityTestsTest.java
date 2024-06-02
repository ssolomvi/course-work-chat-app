import org.junit.jupiter.api.Test;
import ru.mai.primality_test.ProbabilisticPrimalityTest;
import ru.mai.primality_test.impl.ProbabilisticPrimalityTestFermat;
import ru.mai.primality_test.impl.ProbabilisticPrimalityTestMillerRabin;
import ru.mai.primality_test.impl.ProbabilisticPrimalityTestSolovayStrassen;
import ru.mai.utils.MathOperationsBigInteger;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;

public class ProbabilisticPrimalityTestsTest {
    @Test
    public void testProbabilisticPrimalityTests() {
        var testFermat = new ProbabilisticPrimalityTestFermat();
        var testMillerRabin = new ProbabilisticPrimalityTestMillerRabin();
        var testSolovayStrassen = new ProbabilisticPrimalityTestSolovayStrassen();
        int countNumber = 100;
        double minProbability = 0.99999;

        try (FileWriter outputFile = new FileWriter("src/test/resources/testProbabilisticPrimalityTests.txt")) {
            oneProbabilisticPrimalityTest(testFermat, countNumber, minProbability, outputFile);
            oneProbabilisticPrimalityTest(testMillerRabin, countNumber, minProbability, outputFile);
            oneProbabilisticPrimalityTest(testSolovayStrassen, countNumber, minProbability, outputFile);
            outputFile.write("----------------------------------------------------------\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void oneProbabilisticPrimalityTest(ProbabilisticPrimalityTest test, int countNumber, double minProbability, FileWriter outputFile) throws IOException {
        int countOfWrongResults = 0;
        BigInteger upperLimit = new BigInteger("10000000000000");
        ArrayList<Integer> arrayOfIndexes = new ArrayList<>();

        outputFile.write("----------------------------------------------------------\n");
        outputFile.write("Type of test: " + test.getClass().getSimpleName() + "\n\n");

        for (int i = 1; i < countNumber + 1; i++) {
            BigInteger number = MathOperationsBigInteger.generateRandomBigInteger(upperLimit);

            boolean probabilisticTestResult = test.probabilisticPrimalityTest(number, minProbability);
            boolean trulyResult = MathOperationsBigInteger.isPrime(number);

            outputFile.write(i + ") " + number + ": " + probabilisticTestResult
                    + " --- " + trulyResult + ";" + "\n");

            if (probabilisticTestResult != trulyResult) {
                countOfWrongResults++;
                arrayOfIndexes.add(i);
            }
        }

        outputFile.write("\n" + "Count of wrong results: " + countOfWrongResults);
        outputFile.write("\n" + "Indexes of wrong results: " + arrayOfIndexes + "\n");
    }
}
