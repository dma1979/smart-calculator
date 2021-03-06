package calculator;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Calculator logic.
 *
 * @author dzmitry.marudau
 * @since 2020.4
 */
public class Calculator {
    public static final String ASSIGNMENT_DELIMITER_REGEX = "\\s*=\\s*";
    public static final String LETTERS_REGEX = "[A-Za-z]+";
    public static final String NUMBERS_REGEX = "-?+[\\d]+";
    private static final String MINUS_NUMBER = "[-]{2,}";

    private static final Map<String, Integer> PRECEDENCE =
        Map.of("+", 0, "-", 0, "*", 1, "/", 1);

    private static final String COMMAND_HELP = "/help";

    private final Map<String, BigInteger> variables = new HashMap<>();

    public Map<String, BigInteger> getVariables() {
        return variables;
    }

    public void assign(String input) {
        String[] strings = input.trim().split(ASSIGNMENT_DELIMITER_REGEX);
        String name = strings[0].trim();
        String value = strings[1].trim();

        if (value.matches(NUMBERS_REGEX)) {
            variables.put(name, new BigInteger(value));
        } else if (value.matches(LETTERS_REGEX)) {
            if (variables.containsKey(value)) {
                variables.put(name, variables.get(value));
            } else {
                System.out.println("Unknown variable");
            }
        }
    }

    public BigInteger calculate(String input) {
        String[] elements = input.trim().replaceAll(" {2,}", " ").split(" ");
        BigInteger sum = BigInteger.ZERO;
        char sign = '+';
        for (String element : elements) {
            BigInteger value;
            if (element.matches(LETTERS_REGEX)) {
                value = resolveTheValue(element);
                sum = value;
            } else if (element.matches(NUMBERS_REGEX)) {
                value = new BigInteger(element);
                switch (sign) {
                case '+':
                    sum = sum.add(value);
                    break;
                case '-':
                    sum = sum.subtract(value);
                    break;
                case '*':
                    sum = sum.multiply(value);
                    break;
                case '/':
                    sum = sum.divide(value);
                    break;
                }
            } else {
                sign = determineSign(element);
            }
        }
        return sum;
    }

    public BigInteger calculateInPostfixNotation(String expressionInPostfixNotation) {
        String[] elements = expressionInPostfixNotation.trim().replaceAll(" {2,}", " ").split(" ");
        return calculateInPostfixNotation(Arrays.asList(elements));
    }

    public BigInteger calculateInPostfixNotation(List<String> elements) {
        Stack<BigInteger> stack = new Stack<>();
        for (String element : elements) {
            if (element.matches(NUMBERS_REGEX)) {
                //If the incoming element is a number, push it into the stack
                stack.push(new BigInteger(element));
            } else if (element.matches(LETTERS_REGEX)) {
                //If the incoming element is the name of a variable, push its value into the stack.
                stack.push(resolveTheValue(element));
            } else {
                //If the incoming element is an operator, then pop twice to get two numbers
                // and perform the operation; push the result on the stack.
                BigInteger a = stack.pop();
                BigInteger b = stack.pop();
                switch (element.charAt(0)) {
                case '+':
                    stack.push(b.add(a));
                    break;
                case '-':
                    stack.push(b.subtract(a));
                    break;
                case '*':
                    stack.push(b.multiply(a));
                    break;
                case '/':
                    stack.push(b.divide(a));
                    break;
                }
            }
        }
        //When the expression ends, the number on the top of the stack is a final result.
        return stack.pop();
    }

    public Optional<String> getVariableValue(String key) {
        return Optional.ofNullable(variables.get(key)).map(String::valueOf);
    }

    public Optional<String> getCommandInfo(String input) {
        if (COMMAND_HELP.equalsIgnoreCase(input)) {
            return Optional.of("The program calculates simple Math operations");
        }
        return Optional.empty();
    }

    private BigInteger resolveTheValue(String variable) {
        return variables.get(variable) != null
               ? variables.get(variable)
               : BigInteger.ZERO;
    }

    private char determineSign(String input) {
        char sign;
        if (input.matches(MINUS_NUMBER)) {
            sign = input.length() % 2 != 0 ? '-' : '+';
        } else {
            sign = input.charAt(0);
        }
        return sign;
    }

    private String normalizeSign(String input) {
        if (input.matches(MINUS_NUMBER)) {
            return input.length() % 2 != 0 ? "-" : "+";
        }
        return input;
    }

    public List<String> infixToPostfix(String input) throws IllegalArgumentException {
        List<String> strings = normalize(input);
        return infixToPostfix(strings);
    }

    private List<String> normalize(String input) {
        String[] strings = input.trim()
                              .replaceAll("\\(", " ( ")
                              .replaceAll("\\)", " ) ")
                              .replaceAll("\\+{2,}", "+")
                              .replaceAll("\\+", " + ")
                              .replaceAll("\\*", " * ")
                              .replaceAll("\\/", " / ")
                              .replaceAll(" {2,}", " ")
                              .split(" ");

        List<String> result = Arrays.asList(strings);
        result.replaceAll(this::normalizeSign);
        return result;
    }

    private List<String> infixToPostfix(List<String> input) throws IllegalArgumentException {
        Stack<String> stack = new Stack<>();
        List<String> result = new ArrayList<>();
        for (String incoming : input) {
            if (incoming.matches(NUMBERS_REGEX) || incoming.matches(LETTERS_REGEX)) {
                //Add operands (numbers and variables) to the result (postfix notation) as they arrive.
                result.add(incoming);
                continue;
            }

            //If the stack is empty push the incoming operator on the stack.
            if (stack.empty()) {
                stack.push(incoming);
                continue;
            }

            // If the stack contains a left parenthesis on top, push the incoming operator on the stack.
            if (stack.peek().equals("(")) {
                stack.push(incoming);
                continue;
            }

            //If the incoming element is a left parenthesis, push it on the stack.
            if (incoming.equals("(")) {
                stack.push(incoming);
                continue;
            }

            // If the incoming element is a right parenthesis,
            // pop the stack and add operators to the result until you see a left parenthesis.
            // Discard the pair of parentheses.
            if (incoming.equals(")")) {
                String current;
                do {
                    result.add(stack.pop());
                    current = stack.peek();
                }
                while (!current.equals("("));
                stack.pop();

                continue;
            }

            String top = stack.peek();
            if (PRECEDENCE.get(incoming).compareTo(PRECEDENCE.get(top)) > 0) {
                //If the incoming operator has higher precedence than the top of the stack, push it on the stack.
                stack.push(incoming);
            } else {
                //If the incoming operator has lower or equal precedence than the top of the operator stack,
                // pop the stack and add operators to the result
                // until you see an operator that has a smaller precedence
                // or a left parenthesis on the top of the stack;
                // then add the incoming operator to the stack.
                String current;
                do {
                    current = stack.peek();
                    result.add(stack.pop());
                }
                while (!stack.empty() && !(current.equals("(") || PRECEDENCE.get(current).compareTo(PRECEDENCE.get(incoming)) > 0));
                stack.push(incoming);
            }

        }

        //At the end of the expression, pop the stack and add all operators to the result.
        List<String> allOperandsOnStack = stack.stream().filter("+-*/"::contains)
                                                .collect(Collectors.toList());
        Collections.reverse(allOperandsOnStack);
        result.addAll(allOperandsOnStack);
        stack.removeAll(allOperandsOnStack);

        //No parentheses should remain on the stack. Otherwise, the expression has unbalanced brackets.
        // It is a syntax error.
        if (!stack.empty()) {
            throw new IllegalArgumentException("Incorrect expressions");
        }

        return result;

    }

}
