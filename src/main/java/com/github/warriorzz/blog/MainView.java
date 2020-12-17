package com.github.warriorzz.blog;

import com.github.warriorzz.blog.db.DataBase;
import com.github.warriorzz.blog.util.Post;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
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
import io.github.cdimascio.dotenv.Dotenv;

import java.io.*;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The main view is a top-level placeholder for other views.
 */
@Route("")
@JsModule("./styles/shared-styles.js")
@CssImport("./styles/views/main/main-view.css")
@PWA(name = "sz-blog-vaadin", shortName = "sz-blog-vaadin", enableInstallPrompt = false)
public class MainView extends VerticalLayout implements HasDynamicTitle, BeforeEnterObserver {

    private boolean initialized = false;
    private volatile Post currentPost;
    private final VerticalLayout contentGoesHere = new VerticalLayout();

    private Accordion accordion;
    private Tabs impressumEtc;

    private final HashMap<String, Tabs> tabsList = new HashMap<>();
    private final HashMap<Tabs, HashMap<Tab, Post>> layoutMap = new HashMap<>();

    public MainView() {

    }

    private void refresh() {
        setCurrentPostToStartArticle();

        layoutMap.keySet().forEach(it -> accordion.remove(it));
        tabsList.clear();
        layoutMap.clear();

        for(String category: DataBase.getInstance().getCategories()) addCategory(category);

        // TODO: Tabs sortieren
        if(DataBase.getInstance().getPosts() == null) return;
        for(Post post: DataBase.getInstance().getPosts().stream().sorted().collect(Collectors.toList())) {
            if(!post.isConfirmed()) continue;
            for(String tabs: tabsList.keySet()) {
                if(tabs.equals(post.getCategory())) {
                    System.out.println("Test2");
                    Tab tab = new Tab(post.getTitle());
                    layoutMap.get(tabsList.get(post.getCategory())).put(tab, post);
                    tabsList.get(post.getCategory()).add(tab);
                }
            }
        }

        for(Tabs tabs: tabsList.values()) {
            tabs.addSelectedChangeListener((ComponentEventListener<Tabs.SelectedChangeEvent>) event -> {
                if(event.getSelectedTab() == null) return;
                if(layoutMap.get(tabs).containsKey(event.getSelectedTab()))
                    currentPost = layoutMap.get(tabs).get(event.getSelectedTab());
                refreshPost();
                for(Tabs tabs2: tabsList.values()){
                    if(tabs != tabs2){
                        tabs2.setSelectedTab(null);
                    }
                }
                impressumEtc.setSelectedTab(null);
            });
        }
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
        accordionLayout.setId("sidebar-layout");

        accordion = new Accordion();
        accordion.setId("accordion");
        accordionLayout.add(accordion);

        accordion.close();

        impressumEtc = new Tabs();
        accordionLayout.add(impressumEtc);
        impressumEtc.setOrientation(Tabs.Orientation.VERTICAL);

        Tab startArticle = new Tab("Unser Blog!");
        impressumEtc.add(startArticle);

        Tab impressum = new Tab("Impressum");
        impressumEtc.add(impressum);

        Tab intern = new Tab("Intern");
        impressumEtc.add(intern);

        impressumEtc.addSelectedChangeListener((ComponentEventListener<Tabs.SelectedChangeEvent>) event -> {
            if(event.getSelectedTab() == null)
                return;
            if(event.getSelectedTab().equals(startArticle)){
                setCurrentPostToStartArticle();
            }
            if(event.getSelectedTab().equals(impressum)){
                setCurrentPostToImpressum();
            }
            if(event.getSelectedTab().equals(intern)) UI.getCurrent().navigate("intern");
            for(Tabs tabs2: tabsList.values()){
                if(impressumEtc != tabs2){
                    tabs2.setSelectedTab(null);
                }
            }
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
        if(!currentPost.getAuthor().equals("")) titleAndAuthor.add(text);

        Paragraph times = new Paragraph(currentPost.getCreated() + ((currentPost.getLastUpdate() != null) ? (", zuletzt bearbeitet: " + currentPost.getLastUpdate()) : ""));
        times.setId("post-times");
        contentGoesHere.add(titleAndAuthor);
        contentGoesHere.add(currentPost.getLayout());
        if(currentPost.getCreated() != null) contentGoesHere.add(times);
    }

    private void addCategory(String name) {
        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.setAutoselect(false);
        accordion.add(name, tabs);
        tabsList.put(name, tabs);
        layoutMap.put(tabs, new HashMap<>());
    }

    private void setCurrentPostToStartArticle(){
        currentPost = DataBase.getInstance().getPosts().stream().filter(post -> post.getCategory().equals("")).filter(post -> post.getTitle().equals(Dotenv.load().get("START_ARTICLE_NAME"))).findFirst().get();
        refreshPost();
    }

    private void setCurrentPostToImpressum() {
        currentPost = DataBase.getInstance().getPosts().stream().filter(post -> post.getCategory().equals("")).filter(post -> post.getTitle().equals(Dotenv.load().get("IMPRESSUM_NAME"))).findFirst().get();
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
        refresh();
    }
}
