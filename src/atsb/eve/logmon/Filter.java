package atsb.eve.logmon;

import javafx.beans.property.SimpleStringProperty;

public class Filter {

	private final SimpleStringProperty expression;

	public Filter(String expression) {
		this.expression = new SimpleStringProperty(expression);
	}

	public String getExpression() {
		return expression.get();
	}

	public void setExpression(String expression) {
		this.expression.set(expression);
	}

}
