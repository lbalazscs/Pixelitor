package pixelitor;

import org.junit.Test;
import pixelitor.guides.GuideStyle;
import pixelitor.guides.GuidesRenderer;

import static org.assertj.core.api.Assertions.assertThat;

public class DIContainerTest {

    @Test
    public void testGetReturnSameObject() {
        GuideStyle gs1 = DIContainer.get(GuideStyle.class);
        GuideStyle gs2 = DIContainer.get(GuideStyle.class);
        assertThat(gs1).isSameAs(gs2);
    }

    @Test
    public void testGetReturnSameObjectForSameId() {
        GuidesRenderer gr1 = DIContainer.get(GuidesRenderer.class, DIContainer.GUIDES_RENDERER);
        GuidesRenderer gr2 = DIContainer.get(GuidesRenderer.class, DIContainer.GUIDES_RENDERER);
        assertThat(gr1).isSameAs(gr2);

        GuidesRenderer gr3 = DIContainer.get(GuidesRenderer.class, DIContainer.CROP_GUIDES_RENDERER);
        assertThat(gr3).isNotSameAs(gr2);
    }
}
