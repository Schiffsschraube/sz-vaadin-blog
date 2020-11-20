package com.github.warriorzz.blog;

import com.github.warriorzz.blog.load.Loader;
import com.github.warriorzz.blog.util.Post;
import com.github.warriorzz.blog.util.PostBuilder;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.StreamResource;

import java.io.*;
import java.util.HashMap;
import java.util.Objects;

/**
 * The main view is a top-level placeholder for other views.
 */
@Route("")
@JsModule("./styles/shared-styles.js")
@CssImport("./styles/views/main/main-view.css")
@PWA(name = "sz-blog-vaadin", shortName = "sz-blog-vaadin", enableInstallPrompt = false)
public class MainView extends VerticalLayout implements HasDynamicTitle, BeforeEnterObserver {

    /*private final Tabs menu;
    private H1 viewTitle;*/
    private boolean initialized = false;
    private volatile Post currentPost;
    private final VerticalLayout contentGoesHere = new VerticalLayout();
    private final HashMap<Tab, Post> news = new HashMap<>();
    private final HashMap<Tab, Post> blog = new HashMap<>();

    private Tabs tabsBlog;
    private Tabs tabsNews;
    private Tabs impressumEtc;

    public MainView() {

    }

    private void refresh() throws FileNotFoundException, UnsupportedEncodingException {
        setCurrentPostToStartArticle();

        Loader loader = new Loader();

        // TODO: Tabs sortieren

        for(Post post: loader.getPosts()) {
            if("News".equalsIgnoreCase(post.getCategory())){
                Tab tab = new Tab(post.getTitle());
                news.put(tab, post);
                tabsNews.add(tab);
            }
            if("Blog".equalsIgnoreCase(post.getCategory())){
                Tab tab = new Tab(post.getTitle());
                blog.put(tab, post);
                tabsBlog.add(tab);
            }
        }
        tabsBlog.addSelectedChangeListener((ComponentEventListener<Tabs.SelectedChangeEvent>) event -> {
            if(event.getSelectedTab() == null) return;
            if(blog.containsKey(event.getSelectedTab()))
                currentPost = blog.get(event.getSelectedTab());
            refreshPost();
            tabsNews.setSelectedTab(null);
            impressumEtc.setSelectedTab(null);
        });
        tabsNews.addSelectedChangeListener((ComponentEventListener<Tabs.SelectedChangeEvent>) event -> {
            if(event.getSelectedTab() == null) return;
            if(news.containsKey(event.getSelectedTab()))
                currentPost = news.get(event.getSelectedTab());
            refreshPost();
            tabsBlog.setSelectedTab(null);
            impressumEtc.setSelectedTab(null);
        });

    }

    private void initialize() throws IOException {
        setId("layout");

        //HeadBar
        HorizontalLayout headBar = new HorizontalLayout();
        headBar.setId("headbar");

        H1 heading = new H1("schiffsschraube-Blog");
        heading.setId("heading-name");
        headBar.add(heading);

        byte[] imageBytes = Objects.requireNonNull(this.getClass().getClassLoader().getResource("Logo_rot.png")).openStream().readAllBytes();
        StreamResource resource = new StreamResource("Logo_rot.jpg", () -> new ByteArrayInputStream(imageBytes));
        Image image = new Image(resource, "logo");
        image.setId("logo");
        headBar.add(image);

        // Content
        HorizontalLayout content = new HorizontalLayout();

        VerticalLayout accordionLayout = new VerticalLayout();
        accordionLayout.setId("accordion-layout");

        Accordion accordion = new Accordion();
        accordion.setId("accordion");
        accordionLayout.add(accordion);

        accordion.close();

        tabsBlog = new Tabs();
        tabsBlog.setOrientation(Tabs.Orientation.VERTICAL);
        tabsNews = new Tabs();
        tabsNews.setOrientation(Tabs.Orientation.VERTICAL);

        tabsBlog.setAutoselect(false);
        tabsNews.setAutoselect(false);

        accordion.add("Newsticker", tabsNews);
        accordion.add("Blog", tabsBlog);

        impressumEtc = new Tabs();
        accordionLayout.add(impressumEtc);
        impressumEtc.setOrientation(Tabs.Orientation.VERTICAL);

        Tab startArticle = new Tab("Unser Blog!");
        impressumEtc.add(startArticle);

        Tab impressum = new Tab("Impressum");
        impressumEtc.add(impressum);

        impressumEtc.addSelectedChangeListener((ComponentEventListener<Tabs.SelectedChangeEvent>) event -> {
            if(event.getSelectedTab() == null)
                return;
            if(event.getSelectedTab().equals(startArticle)){
                setCurrentPostToStartArticle();
            }
            if(event.getSelectedTab().equals(impressum)){
                setCurrentPostToImpressum();
            }
            tabsBlog.setSelectedTab(null);
            tabsNews.setSelectedTab(null);
        });
        impressumEtc.setSelectedTab(startArticle);

        content.add(accordionLayout);
        content.add(contentGoesHere);
        add(headBar);
        add(content);
    }

    private void refreshPost() {
        contentGoesHere.removeAll();

        H2 heading = new H2(currentPost.getTitle());
        heading.setId("post-heading");
        Span text = new Span("von " + currentPost.getAuthor());
        text.setId("post-author");
        HorizontalLayout titleAndAuthor = new HorizontalLayout();
        titleAndAuthor.add(heading);
        if(currentPost.getAuthor() != null) titleAndAuthor.add(text);

        Paragraph times = new Paragraph(currentPost.getCreated() + ((currentPost.getLastUpdate() != null) ? (", zuletzt bearbeitet: " + currentPost.getLastUpdate()) : ""));
        times.setId("post-times");
        contentGoesHere.add(titleAndAuthor);
        contentGoesHere.add(currentPost.getLayout());
        if(currentPost.getCreated() != null) contentGoesHere.add(times);

    }

    private void setCurrentPostToStartArticle(){
        currentPost = new PostBuilder().title("Unser Blog!").category("Blog").text("Hier findet ihr unseren neuen Blog! Bei Fragen, Anmerkungen und Kritik, meldet euch bitte unter schiffsschraube@whgw.de! :)").build();
        refreshPost();
    }

    private void setCurrentPostToImpressum() {
        currentPost = new PostBuilder().title("Impressum").text("-- Text ---").build();
        refreshPost();
    }

    @Override
    public String getPageTitle() {
        return "schiffsschraube - " + currentPost.getTitle();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if(!initialized){
            try {
                initialize();
            } catch (IOException e) {
                e.printStackTrace();
            }
            initialized = true;
        }
        try {
            refresh();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
