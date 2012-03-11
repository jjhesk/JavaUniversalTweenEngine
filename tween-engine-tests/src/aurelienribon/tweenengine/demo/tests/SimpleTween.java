package aurelienribon.tweenengine.demo.tests;

import aurelienribon.accessors.SpriteAccessor;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenManager;
import aurelienribon.tweenengine.demo.Test;
import aurelienribon.tweenengine.equations.Cubic;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com
 */
public class SimpleTween extends Test {
	private final TweenManager tweenManager = new TweenManager();

	@Override
	public String getTitle() {
		return "Simple Tween";
	}

	@Override
	public String getInfo() {
		return "A 'tween' is an interpolation from a value to an other. "
			+ "(Click anywhere to start a 'position tween')";
	}

	@Override
	public String getImageName() {
		return "tile-tween";
	}

	@Override
	public InputProcessor getInput() {
		return inputProcessor;
	}

	@Override
	protected void initializeOverride() {
		createSprites(1);
		enableDots(0);
		center(sprites[0], 0, 0);
	}

	@Override
	protected void disposeOverride() {
		tweenManager.killAll();
	}

	@Override
	protected void renderOverride() {
		tweenManager.update(Gdx.graphics.getDeltaTime());
	}

	private final InputProcessor inputProcessor = new InputAdapter() {
		@Override
		public boolean touchDown(int x, int y, int pointer, int button) {
			Vector2 v = touch2world(x, y);

			tweenManager.killAll();

			Tween.to(sprites[0], SpriteAccessor.CPOS_XY, 0.6f)
				.target(v.x, v.y)
				.ease(Cubic.INOUT)
				.start(tweenManager);

			return true;
		}
	};
}
