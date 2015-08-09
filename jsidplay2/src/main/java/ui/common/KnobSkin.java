package ui.common;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.SkinBase;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;

import com.sun.javafx.scene.control.behavior.SliderBehavior;

/**
 * A simple knob skin for slider
 * 
 * @author Jasper Potts
 */
public class KnobSkin extends SkinBase<Slider> {

	private double knobRadius;
	private double minAngle = -140;
	private double maxAngle = 140;
	protected double dragOffset;

	protected StackPane knob;
	private StackPane knobOverlay;
	protected StackPane knobDot;

	public KnobSkin(Slider slider) {
		super(slider);
		initialize();
	}

	private void initialize() {
		knob = new StackPane() {
			@Override
			protected void layoutChildren() {
				knobDot.setLayoutX((knob.getWidth() - knobDot.getWidth()) / 2);
				knobDot.setLayoutY(5 + (knobDot.getHeight() / 2));
			}

		};
		knob.getStyleClass().setAll("knob");
		knobOverlay = new StackPane();
		knobOverlay.getStyleClass().setAll("knobOverlay");
		knobDot = new StackPane();
		knobDot.getStyleClass().setAll("knobDot");

		getChildren().setAll(knob, knobOverlay);
		knob.getChildren().add(knobDot);

		SliderBehavior behavior = new SliderBehavior(getSkinnable());

		getSkinnable().setOnKeyPressed((ke) -> getSkinnable().requestLayout());
		getSkinnable().setOnKeyReleased((ke) -> getSkinnable().requestLayout());
		getSkinnable()
				.setOnMousePressed(
						me -> {
							double dragStart = mouseToValue(me.getX(),
									me.getY());
							double zeroOneValue = (getSkinnable().getValue() - getSkinnable()
									.getMin())
									/ (getSkinnable().getMax() - getSkinnable()
											.getMin());
							dragOffset = zeroOneValue - dragStart;
							behavior.thumbPressed(me, dragStart);
							behavior.trackPress(me, dragStart);
							getSkinnable().requestLayout();
						});
		getSkinnable().setOnMouseReleased(me -> behavior.thumbReleased(me));
		getSkinnable().setOnMouseDragged(
				me -> {
					behavior.thumbDragged(me,
							mouseToValue(me.getX(), me.getY()) + dragOffset);
					getSkinnable().requestLayout();
				});
		getSkinnable().valueProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, Number newValue) {
				getSkinnable().requestLayout();
			}
		});
	}

	private double mouseToValue(double mouseX, double mouseY) {
		double cx = getSkinnable().getWidth() / 2;
		double cy = getSkinnable().getHeight() / 2;
		double mouseAngle = Math.toDegrees(Math.atan((mouseY - cy)
				/ (mouseX - cx)));
		double topZeroAngle;
		if (mouseX < cx) {
			topZeroAngle = 90 - mouseAngle;
		} else {
			topZeroAngle = -(90 + mouseAngle);
		}
		double value = 1 - ((topZeroAngle - minAngle) / (maxAngle - minAngle));
		return value;
	}

	private void rotateKnob() {
		Slider s = getSkinnable();
		double zeroOneValue = (s.getValue() - s.getMin())
				/ (s.getMax() - s.getMin());
		double angle = minAngle + ((maxAngle - minAngle) * zeroOneValue);
		knob.setRotate(angle);
	}

	@Override
	protected void layoutChildren(double x, double y, double w, double h) {
		// calculate the available space
		double cx = x + (w / 2);
		double cy = y + (h / 2);

		// resize thumb to preferred size
		double knobWidth = knob.prefWidth(-1);
		double knobHeight = knob.prefHeight(-1);
		knobRadius = Math.max(knobWidth, knobHeight) / 2;
		knob.resize(knobWidth, knobHeight);
		knob.setLayoutX(cx - knobRadius);
		knob.setLayoutY(cy - knobRadius);
		knobOverlay.resize(knobWidth, knobHeight);
		knobOverlay.setLayoutX(cx - knobRadius);
		knobOverlay.setLayoutY(cy - knobRadius);
		rotateKnob();
	}

	@Override
	protected double computeMinWidth(double height, double topInset,
			double rightInset, double bottomInset, double leftInset) {
		return (leftInset + knob.minWidth(-1) + rightInset);
	}

	@Override
	protected double computeMinHeight(double width, double topInset,
			double rightInset, double bottomInset, double leftInset) {
		return (topInset + knob.minHeight(-1) + bottomInset);
	}

	@Override
	protected double computePrefWidth(double height, double topInset,
			double rightInset, double bottomInset, double leftInset) {
		return (leftInset + knob.prefWidth(-1) + rightInset);
	}

	@Override
	protected double computePrefHeight(double width, double topInset,
			double rightInset, double bottomInset, double leftInset) {
		return (topInset + knob.prefHeight(-1) + bottomInset);
	}

	@Override
	protected double computeMaxWidth(double height, double topInset,
			double rightInset, double bottomInset, double leftInset) {
		return Double.MAX_VALUE;
	}

	@Override
	protected double computeMaxHeight(double width, double topInset,
			double rightInset, double bottomInset, double leftInset) {
		return Double.MAX_VALUE;
	}

}
