import java.util.*;
import java.util.regex.*;

public class OGNLExpressionsClaude {
    
	 public static List<Map<String, Object>> generateTestData(String ognlExpression, boolean isPositive, boolean triggerErrors) {
	        List<Map<String, Object>> testDataList = new ArrayList<>();
	        
	        // Extract ternary variable
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
	            } else {
	                // Handle equality conditions
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
	            testCase.put(ternaryVar, isPositive ? "ValidValue" : null);
	            if (!isPositive && triggerErrors) {
	                testCase.put("errorMessage", ternaryVar + " cannot be null.");
	            }
	        }
	    }

	    public static void main(String[] args) {
	        String ognlExpression = "(#user.age in {23, 24, 28} || (#user.age == 30 && #user.country == 'USA')) ? #user.status != null : true";

	        // Generate Test Data
	        System.out.println("✅ Positive Test Data: " + generateTestData(ognlExpression, true, false));
	        System.out.println("❌ Negative Test Data (Error-Triggered): " + generateTestData(ognlExpression, false, true));
	    }
}
