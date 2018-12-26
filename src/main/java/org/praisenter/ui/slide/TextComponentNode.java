package org.praisenter.ui.slide;

import org.praisenter.data.slide.effects.SlideShadow;
import org.praisenter.data.slide.graphics.SlidePadding;
import org.praisenter.data.slide.graphics.SlidePaint;
import org.praisenter.data.slide.graphics.SlideStroke;
import org.praisenter.data.slide.text.FontScaleType;
import org.praisenter.data.slide.text.HorizontalTextAlignment;
import org.praisenter.data.slide.text.SlideFont;
import org.praisenter.data.slide.text.TextComponent;
import org.praisenter.data.slide.text.VerticalTextAlignment;
import org.praisenter.ui.GlobalContext;
import org.praisenter.ui.TextMeasurer;
import org.praisenter.ui.slide.convert.BorderConverter;
import org.praisenter.ui.slide.convert.EffectConverter;
import org.praisenter.ui.slide.convert.FontConverter;
import org.praisenter.ui.slide.convert.PaintConverter;
import org.praisenter.ui.slide.convert.TextAlignmentConverter;

import javafx.beans.binding.Bindings;
import javafx.scene.effect.Effect;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;

final class TextComponentNode extends SlideComponentNode<TextComponent> {
	/** A wrapper node for the text node (used to apply padding and vertical alignment) */
	private final VBox wrapper;
	
	/** The text */
	private final Text text;
	
	public TextComponentNode(GlobalContext context, TextComponent region) {
		super(context, region);
		
		this.wrapper = new VBox();
		this.text = new Text();
		
		this.text.setBoundsType(TextBoundsType.LOGICAL);
		this.wrapper.getChildren().add(this.text);
		this.content.getChildren().add(this.wrapper);
		
		// listen for changes
		
		// TODO for placeholders or any text component that doesn't have text we should show a default (but only in edit mode) - maybe we can show this on the EditNode
		this.text.textProperty().bind(this.region.textProperty());
		
		this.text.fillProperty().bind(Bindings.createObjectBinding(() -> {
			// NOTE: this should work for now, but won't work in the future if we allow image based text paints
			SlidePaint sp = this.region.getTextPaint();
			return PaintConverter.toJavaFX(sp);
		}, this.region.textPaintProperty()));
		
		this.text.strokeTypeProperty().bind(Bindings.createObjectBinding(() -> {
			SlideStroke ss = this.region.getTextBorder();
			if (ss == null) return StrokeType.CENTERED;
			return BorderConverter.toJavaFX(ss.getStyle().getType());
		}, this.region.textBorderProperty()));

		this.text.strokeLineCapProperty().bind(Bindings.createObjectBinding(() -> {
			SlideStroke ss = this.region.getTextBorder();
			if (ss == null) return StrokeLineCap.SQUARE;
			return BorderConverter.toJavaFX(ss.getStyle().getCap());
		}, this.region.textBorderProperty()));
		
		this.text.strokeLineJoinProperty().bind(Bindings.createObjectBinding(() -> {
			SlideStroke ss = this.region.getTextBorder();
			if (ss == null) return StrokeLineJoin.MITER;
			return BorderConverter.toJavaFX(ss.getStyle().getJoin());
		}, this.region.textBorderProperty()));
		
		this.text.strokeWidthProperty().bind(Bindings.createObjectBinding(() -> {
			SlideStroke ss = this.region.getTextBorder();
			if (ss == null) return 0;
			return ss.getWidth();
		}, this.region.textBorderProperty()));
		
		this.text.strokeProperty().bind(Bindings.createObjectBinding(() -> {
			SlideStroke ss = this.region.getTextBorder();
			if (ss == null) return Color.TRANSPARENT;
			return PaintConverter.toJavaFX(ss.getPaint());
		}, this.region.textBorderProperty()));

		// TODO we don't have miter limit
		// TODO we don't have stroke dash offset
		
		// TODO need a fix for binding the dashes
		//Bindings.bindContent(text.getStrokeDashArray(), this.region.borderProperty().get().getStyle().dashesProperty());
		
		this.text.fontProperty().bind(Bindings.createObjectBinding(() -> {
			SlideFont font = this.region.getFont();
			return FontConverter.toJavaFX(font);
		}, this.region.fontProperty()));
		
		this.text.textAlignmentProperty().bind(Bindings.createObjectBinding(() -> {
			HorizontalTextAlignment ha = this.region.getHorizontalTextAlignment();
			return TextAlignmentConverter.toJavaFX(ha);
		}, this.region.horizontalTextAlignmentProperty()));
		
		this.wrapper.alignmentProperty().bind(Bindings.createObjectBinding(() -> {
			HorizontalTextAlignment ha = this.region.getHorizontalTextAlignment();
			VerticalTextAlignment va = this.region.getVerticalTextAlignment();
			return TextAlignmentConverter.toJavaFX(va, ha);
		}, this.region.horizontalTextAlignmentProperty()));
		
		this.wrapper.paddingProperty().bind(Bindings.createObjectBinding(() -> {
			SlidePadding sp = this.region.getPadding();
			return TextAlignmentConverter.toJavaFX(sp);
		}, this.region.paddingProperty()));
		
		this.text.wrappingWidthProperty().bind(Bindings.createDoubleBinding(() -> {
			return this.computeWrappingWidth();
		}, this.region.paddingProperty(), this.region.widthProperty(), this.region.textWrappingEnabledProperty()));
		
		this.text.lineSpacingProperty().bind(this.region.lineSpacingProperty());
		
		// the font is based on a lot of factors
		this.text.fontProperty().bind(Bindings.createObjectBinding(() -> {
			return this.computeFont();
		}, this.region.fontProperty(), 
			this.region.fontScaleTypeProperty(), 
			this.region.widthProperty(), 
			this.region.heightProperty(), 
			this.region.paddingProperty(), 
			this.region.lineSpacingProperty(), 
			this.region.textWrappingEnabledProperty(),
			this.region.textProperty()));
		
		this.text.effectProperty().bind(Bindings.createObjectBinding(() -> {
			return this.computeTextEffect();
		}, this.region.textShadowProperty(), this.region.textGlowProperty()));
	}
	
	private double computeWrappingWidth() {
		double w = this.region.getWidth();
		SlidePadding sp = this.region.getPadding();
		boolean isTextWrapping = this.region.isTextWrappingEnabled();

		// compute the bounding text width and height so 
		// we can compute an accurate font size
		double pw = w - sp.getLeft() - sp.getRight();
		
		// set the wrapping width and the bounds type
		if (isTextWrapping) {
			return pw;
		} else {
			return 0.0;
		}
	}
	
	private Effect computeTextEffect() {
		SlideShadow ss = this.region.getTextShadow();
		SlideShadow sg = this.region.getGlow();
		EffectBuilder builder = EffectBuilder.create();
		Effect shadow = EffectConverter.toJavaFX(ss);
		Effect glow = EffectConverter.toJavaFX(sg);
		builder.add(shadow, shadow != null && shadow instanceof InnerShadow ? 10 : 30);
		builder.add(glow, glow != null && glow instanceof InnerShadow ? 20 : 40);
		return builder.build();
	}
	
	private Font computeFont() {
		double w = this.region.getWidth();
		double h = this.region.getHeight();
		
		// compute the bounding text width and height so 
		// we can compute an accurate font size
		SlidePadding padding = this.region.getPadding();
		double pw = Math.max(1.0, w - padding.getLeft() - padding.getRight());
		double ph = Math.max(1.0, h - padding.getTop() - padding.getBottom());
		
		FontScaleType scaleType = this.region.getFontScaleType();
		double lineSpacing = this.region.getLineSpacing();
		boolean isWrapping = this.region.isTextWrappingEnabled();
		
		String str = this.region.getText();
		
		// compute a fitting font, if necessary
		SlideFont sf = this.region.getFont();
		Font base = FontConverter.toJavaFX(sf);
		Font font = base;
		if (scaleType == FontScaleType.REDUCE_SIZE_ONLY) {
			if (isWrapping) {
				font = TextMeasurer.getFittingFontForParagraph(str, base, base.getSize(), pw, ph, lineSpacing, TextBoundsType.LOGICAL);
			} else {
				font = TextMeasurer.getFittingFontForLine(str, base, base.getSize(), pw, TextBoundsType.LOGICAL);
			}
		} else if (scaleType == FontScaleType.BEST_FIT) {
			if (isWrapping) {
				font = TextMeasurer.getFittingFontForParagraph(str, base, Double.MAX_VALUE, pw, ph, lineSpacing, TextBoundsType.LOGICAL);
			} else {
				font = TextMeasurer.getFittingFontForLine(str, base, Double.MAX_VALUE, pw, TextBoundsType.LOGICAL);
			}
		}
		
		return font;
	}
}