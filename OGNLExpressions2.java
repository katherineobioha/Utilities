package utilitiesMaven;
import java.util.*;
import java.util.regex.*;

public class OGNLExpression {

    private static final Random random = new Random();

    public static List<Map<String, Object>> generateTestData(String ognlExpression, boolean isPositive, boolean triggerErrors) {
        List<Map<String, Object>> testDataList = new ArrayList<>();
        
        // Handle the variable comparison expression: #user.age != null ? #user.age != #user.class : true
        if (ognlExpression.matches(".*#\\w+\\.\\w+\\s*!=\\s*null\\s*\\?\\s*#\\w+\\.\\w+\\s*!=\\s*#\\w+\\.\\w+.*")) {
            // Extract variables
            Matcher nullCheckMatcher = Pattern.compile("#\\w+\\.(\\w+)\\s*!=\\s*null").matcher(ognlExpression);
            nullCheckMatcher.find();
            String nullCheckVar = nullCheckMatcher.group(1);
            
            Matcher comparisonMatcher = Pattern.compile("#\\w+\\.(\\w+)\\s*!=\\s*#\\w+\\.(\\w+)").matcher(ognlExpression);
            comparisonMatcher.find();
            String leftVar = comparisonMatcher.group(1);
            String rightVar = comparisonMatcher.group(2);
            
            // Generate positive test data (var != null AND var1 != var2)
            if (isPositive) {
                // Case 1: Different numeric values
                Map<String, Object> testCase = new HashMap<>();
                int value1 = random.nextInt(100);
                int value2;
                do {
                    value2 = random.nextInt(100);
                } while (value1 == value2);
                
                testCase.put(leftVar, value1);
                testCase.put(rightVar, value2);
                testDataList.add(testCase);
                
                // Case 2: Different types
                testCase = new HashMap<>();
                testCase.put(leftVar, random.nextInt(100));
                testCase.put(rightVar, "Value" + random.nextInt(100));
                testDataList.add(testCase);
            }
            // Generate negative test data
            else {
                // Case 1: First variable is null
                Map<String, Object> testCase = new HashMap<>();
                testCase.put(leftVar, null);
                testCase.put(rightVar, random.nextInt(100));
                if (triggerErrors) {
                    testCase.put("errorMessage", leftVar + " cannot be null");
                }
                testDataList.add(testCase);
                
                // Case 2: Variables have same value
                testCase = new HashMap<>();
                int sameValue = random.nextInt(100);
                testCase.put(leftVar, sameValue);
                testCase.put(rightVar, sameValue);
                if (triggerErrors) {
                    testCase.put("errorMessage", leftVar + " must be different from " + rightVar);
                }
                testDataList.add(testCase);
            }
            
            return testDataList;
        }
        
        // Handle regex pattern matching expression: #user.matches("[0-9]{5}|[0-9]{9}")
        if (ognlExpression.contains("#user.matches(")) {
            String conditionalPart = ognlExpression.split("\\?")[0].trim();
            String regexPart = ognlExpression.split("\\?")[1].trim();
            
            // Process the regex pattern
            Matcher regexMatcher = Pattern.compile("#user\\.matches\\(\"([^\"]+)\"\\)").matcher(regexPart);
            String regexPattern = null;
            if (regexMatcher.find()) {
                regexPattern = regexMatcher.group(1);
            }
            
            // Generate test data for the conditional part
            List<Map<String, Object>> conditionalData = generateTestData(conditionalPart, isPositive, false);
            
            // Add regex match data
            for (Map<String, Object> testCase : conditionalData) {
                if (regexPattern != null) {
                    if (isPositive) {
                        // For positive cases, add a valid matching value
                        if (regexPattern.equals("[0-9]{5}|[0-9]{9}")) {
                            // Generate either 5 or 9 digit number
                            String value = random.nextBoolean() 
                                ? String.format("%05d", random.nextInt(100000))  // 5 digits
                                : String.format("%09d", random.nextInt(1000000000));  // 9 digits
                            testCase.put("matches", value);
                        }
                    } else {
                        // For negative cases, add an invalid value
                        testCase.put("matches", "invalid" + random.nextInt(1000));
                        if (triggerErrors) {
                            testCase.put("errorMessage", "Input does not match required pattern: " + regexPattern);
                        }
                    }
                }
                testDataList.add(testCase);
            }
            
            return testDataList;
        }
        
        // Original expression handling
        // Extract ternary variable for original expression
        String ternaryVar = null;
        if (ognlExpression.contains("?")) {
            Matcher m = Pattern.compile("#\\w+\\.(\\w+)\\s*!=\\s*null").matcher(ognlExpression);
            if (m.find()) ternaryVar = m.group(1);
            ognlExpression = ognlExpression.split("\\?")[0].trim();
        }

        // Remove outer parentheses and split OR conditions
        ognlExpression = ognlExpression.replaceAll("^\\((.*)\\)$", "$1").trim();
        for (String condition : ognlExpression.split("\\|\\|")) {
            condition = condition.trim().replaceAll("^\\((.*)\\)$", "$1").trim();

            if (condition.contains("in")) {
                // Handle "in" operator
                Matcher m = Pattern.compile("#\\w+\\.(\\w+)\\s+in\\s+\\{([^}]+)\\}").matcher(condition);
                if (m.find()) {
                    String variable = m.group(1);
                    for (String val : m.group(2).split(",")) {
                        Map<String, Object> testCase = new HashMap<>();
                        try {
                            testCase.put(variable, Integer.parseInt(val.trim()));
                        } catch (NumberFormatException e) {
                            testCase.put(variable, val.trim());
                        }
                        addTernaryResult(testCase, ternaryVar, isPositive, triggerErrors);
                        testDataList.add(testCase);
                    }
                }
            } else if (condition.contains("&&")) {
                // Handle AND conditions
                Map<String, Object> testCase = new HashMap<>();
                
                // Extract all equality conditions
                Matcher m = Pattern.compile("#\\w+\\.(\\w+)\\s*==\\s*'?([^'\\s&)]+)'?").matcher(condition);
                while (m.find()) {
                    testCase.put(m.group(1), parseValue(m.group(2)));
                }
                
                if (!testCase.isEmpty()) {
                    addTernaryResult(testCase, ternaryVar, isPositive, triggerErrors);
                    testDataList.add(testCase);
                }
            } else {
                // Handle simple equality condition
                Map<String, Object> testCase = new HashMap<>();
                Matcher m = Pattern.compile("#\\w+\\.(\\w+)\\s*==\\s*'?([^'\\s&)]+)'?").matcher(condition);
                while (m.find()) {
                    testCase.put(m.group(1), parseValue(m.group(2)));
                }
                if (!testCase.isEmpty()) {
                    addTernaryResult(testCase, ternaryVar, isPositive, triggerErrors);
                    testDataList.add(testCase);
                }
            }
        }

        return testDataList;
    }

    private static Object parseValue(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private static void addTernaryResult(Map<String, Object> testCase, String ternaryVar, boolean isPositive, boolean triggerErrors) {
        if (ternaryVar != null) {
            // Generate a random valid value for positive cases
            testCase.put(ternaryVar, isPositive ? generateValidValue() : null);
            if (!isPositive && triggerErrors) {
                testCase.put("errorMessage", ternaryVar + " cannot be null.");
            }
        }
    }
    
    private static Object generateValidValue() {
        // Generate a non-null, valid value - either a string or number
        if (random.nextBoolean()) {
            return "Value" + random.nextInt(1000);
        } else {
            return random.nextInt(1000);
        }
    }

    public static void main(String[] args) {
        // Original test case
        String ognlExpression1 = "(#user.age in {23, 24, 28} || (#user.age == 30 && #user.country == 'USA')) ? #user.status != null : true";
        System.out.println("Original expression:");
        System.out.println("✅ Positive Test Data: " + generateTestData(ognlExpression1, true, false));
        System.out.println("❌ Negative Test Data (Error-Triggered): " + generateTestData(ognlExpression1, false, true));
        
        // Variable comparison expression
        String ognlExpression2 = "#user.age != null ? #user.age != #user.class : true";
        System.out.println("\nVariable comparison expression:");
        System.out.println("✅ Positive Test Data: " + generateTestData(ognlExpression2, true, false));
        System.out.println("❌ Negative Test Data (Error-Triggered): " + generateTestData(ognlExpression2, false, true));
        
        // New regex pattern matching expression
        String ognlExpression3 = "(#user.age in {23, 24, 28} || (#user.age == 30 && #user.country == 'USA')) ? #user.matches(\"[0-9]{5}|[0-9]{9}\") : true";
        System.out.println("\nRegex pattern matching expression:");
        System.out.println("✅ Positive Test Data: " + generateTestData(ognlExpression3, true, false));
        System.out.println("❌ Negative Test Data (Error-Triggered): " + generateTestData(ognlExpression3, false, true));
    }
}
