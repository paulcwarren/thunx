package eu.xenit.contentcloud.thunx.encoding.json;

import eu.xenit.contentcloud.thunx.predicates.model.ThunkExpression;
import eu.xenit.contentcloud.thunx.predicates.model.FunctionExpression.Operator;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
class JsonFunctionDto implements JsonExpressionDto {

    private final String type = "function";
    private String operator;

    private List<JsonExpressionDto> terms = new ArrayList<>();

    public JsonFunctionDto(String operator, List<JsonExpressionDto> terms) {
        this.operator = operator;
        this.terms.addAll(terms);
    }

    public JsonFunctionDto(String operator, Stream<JsonExpressionDto> result) {
        this.operator = operator;
        result.forEach(expr -> terms.add(expr));
    }

    public static JsonFunctionDto of(String operator, JsonExpressionDto left, JsonExpressionDto right) {
        return new JsonFunctionDto(operator, List.of(left, right));
    }

    @Override
    public <T> ThunkExpression<T> toExpression() throws InvalidExpressionDataException {
        // resolve the operator
        // which is the result-type of the operator ?
        // can we instantiate through the operator ?
        var op = Operator.resolve(this.operator);
        var factory = op.getFactory();
        if (factory == null) {
            // NOT implemented yet ?
            throw new UnsupportedOperationException("factory for operator " + op.getKey() + " is not implemented");
        }

        // convert all the terms
        var exprTerms = new ArrayList<ThunkExpression<?>>();
        for (var dto : terms) {
            exprTerms.add(dto.toExpression());
        }

        return factory.create(exprTerms);
    }
}
