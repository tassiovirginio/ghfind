package br.com.tassiovirginio;

import de.agilecoders.wicket.core.Bootstrap;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;

public class WicketApplication extends WebApplication {

    @Override
    public Class<? extends WebPage> getHomePage() {
        return HomePage.class;
    }

    @Override
    public void init() {
        Bootstrap.install(this);
        super.init();
    }
}
