package br.com.tassiovirginio;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.WebPage;
import org.kohsuke.github.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomePage extends WebPage {
    private static final long serialVersionUID = 1L;

    private String token;
    private String language;
    private String stars;
    private String forks;
//    private String tags;
    private String year;
    private String fileContent;
    private String directoryContent;
    private ListView<Repositorio> listview;

    private List<Repositorio> repositoryList;

    private WebMarkupContainer container;

    private Boolean stopProcess = false;

    private AjaxButton btSubmit;
    private AjaxButton btStop;

    private int contEncontrados;
    private int contPesquisados;

    public HomePage() {

        contEncontrados = 0;
        contPesquisados = 0;

        repositoryList = new ArrayList<>();

        token = "";
        language = "java";
        stars = ">100";
        forks = ">10";
//        tags = "";
        year = "2009";
        fileContent = "pom.xml";
        directoryContent = "src/test";

        Form form = new Form<Void>("fom") {
            @Override
            protected void onSubmit() {

            }
        };

        form.add(new TextField<String>("token", PropertyModel.of(this, "token")));
        form.add(new TextField<String>("language", PropertyModel.of(this, "language")));
        form.add(new TextField<String>("stars", PropertyModel.of(this, "stars")));
        form.add(new TextField<String>("forks", PropertyModel.of(this, "forks")));
//        form.add(new TextField<String>("tags", PropertyModel.of(this, "tags")));
        form.add(new TextField<String>("year", PropertyModel.of(this, "year")));

        form.add(new TextField<String>("fileContent", PropertyModel.of(this, "fileContent")));
        form.add(new TextField<String>("directoryContent", PropertyModel.of(this, "directoryContent")));

        btSubmit = new AjaxButton("btSubmit") {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                btStop.setEnabled(true);
                target.add(btStop);
                btSubmit.setEnabled(false);
                target.add(btSubmit);
                stopProcess = false;
                Thread thread = new Thread() {
                    public void run() {
                        realizarBusca();
                    }
                };
                thread.start();
            }
        };
        form.add(btSubmit);

        btStop = new AjaxButton("btStop") {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                stopProcess = true;
                btSubmit.setEnabled(true);
                target.add(btSubmit);
                btStop.setEnabled(false);
                target.add(btStop);
            }
        };
        btStop.setEnabled(false);
        form.add(btStop);
        add(form);

        Label pjEncontrados = new Label("pjEncontrados",PropertyModel.of(this, "contEncontrados"));
        pjEncontrados.setOutputMarkupId(true);

        Label pjPesquisados = new Label("pjPesquisados",PropertyModel.of(this, "contPesquisados"));
        pjPesquisados.setOutputMarkupId(true);

        WebMarkupContainer containerInfo = new WebMarkupContainer("containerInfo");
        containerInfo.setOutputMarkupId(true);
        containerInfo.add(new AjaxSelfUpdatingTimerBehavior(Duration.ofSeconds(1)));
        containerInfo.add(pjEncontrados);
        containerInfo.add(pjPesquisados);
        add(containerInfo);


        listview = new ListView<Repositorio>("listview", repositoryList) {
            protected void populateItem(ListItem<Repositorio> item) {
                Repositorio repository = item.getModelObject();
                item.add(new Label("name", repository.getName()));
//                item.add(new Label("homePage", repository.getUrl()));
//                item.add(new Label("fullName", repository.getFullname()));
                item.add(new Label("giturl", repository.getGitUrl()));
                item.add(new Label("dateUpdate", repository.getDateUpdate()));
                item.add(new Label("watches", repository.getWatches()));
                item.add(new Label("forks", repository.getForks()));
                item.add(new Label("stars", repository.getStars()));
            }
        };

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        container.add(new AjaxSelfUpdatingTimerBehavior(Duration.ofSeconds(1)));
        container.add(listview);
        add(container);

    }

    private void realizarBusca() {

        GitHub github = null;
        try {
            if (token == null) {
                github = GitHub.connectAnonymously();
            } else {
                github = new GitHubBuilder().withOAuthToken(token).build();
            }

            GHRepositorySearchBuilder ghRepositorySearchBuilder = github.searchRepositories();

            if (language != null)
                ghRepositorySearchBuilder.language(language);

            if (stars != null)
                ghRepositorySearchBuilder.stars(stars);

            if (forks != null)
                ghRepositorySearchBuilder.forks(forks);

            ghRepositorySearchBuilder.order(GHDirection.DESC);
            ghRepositorySearchBuilder.sort(GHRepositorySearchBuilder.Sort.STARS);

            PagedIterable<GHRepository> lista = ghRepositorySearchBuilder.list();

            lista.withPageSize(100);

            for (GHRepository ghRepository : lista) {
                if(stopProcess)break;

                contPesquisados++;

                System.out.print("\npesquisando: " + ghRepository.getSshUrl());

                Boolean contentPOM = true;

//                ghRepository.listLanguages().containsValue("Java");

                if (fileContent != null) {
                    try {
                        GHContent ghContentPOM = ghRepository.getFileContent(fileContent);

                        if (ghContentPOM != null) {
                            if (ghContentPOM.isFile()) {
                                contentPOM = true;
                                System.out.print(" - É MAVEN");
                            } else {
                                contentPOM = false;
                            }
                        }
                    } catch (Exception e) {
                        System.out.print(" - NÃO É MAVEN");
                        contentPOM = false;
                    }
                }

                Boolean contentFolderTest = true;

                if (directoryContent != null) {
                    try {
                        List<GHContent> listGHContent = ghRepository.getDirectoryContent(directoryContent);
                        if (listGHContent != null) {
                            if (listGHContent.size() > 0) {
                                contentFolderTest = true;
                                System.out.print(" - CONTÉM TESTES");
                            } else {
                                contentFolderTest = false;
                            }
                        }
                    } catch (Exception e) {
                        System.out.print(" - NÃO CONTÉM TESTES");
                        contentFolderTest = false;
                    }
                }

//                Boolean quantidadeStrelas = true;
//                Boolean quantidadesTags = true;
                Boolean dataUpdate = true;


//                if (stars != null) {
//                    quantidadeStrelas = (ghRepository.getStargazersCount() >= Integer.parseInt(stars));
//                }

//                if (tags != null) {
//                    quantidadesTags = ghRepository.listTags() listTags().toList().size() > Integer.parseInt(tags);
//                }

                if (year != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(ghRepository.getUpdatedAt());
                    dataUpdate = calendar.get(Calendar.YEAR) > Integer.parseInt(year);
                }

                if (contentPOM && contentFolderTest) {
                    if (dataUpdate) {
                        System.out.println(" - ###########  ENCONTRADO ###########");
                        Repositorio repositorio = new Repositorio();
                        repositorio.setFullname(ghRepository.getFullName());
                        repositorio.setName(ghRepository.getName());
                        repositorio.setUrl(ghRepository.getHomepage());
                        repositorio.setGitUrl(ghRepository.getHttpTransportUrl());

                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                        repositorio.setDateUpdate(simpleDateFormat.format(ghRepository.getUpdatedAt()));
                        repositorio.setStars(ghRepository.getStargazersCount()+"");
                        repositorio.setForks(ghRepository.getForksCount()+"");
                        repositorio.setWatches(ghRepository.getWatchersCount()+"");

                        listview.getList().add(repositorio);
                        contEncontrados++;
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    class Repositorio implements Serializable {

        private String name;
        private String fullname;
        private String url;
        private String gitUrl;
        private String dateUpdate;
        private String stars;
        private String forks;
        private String watches;

        public String getWatches() {
            return watches;
        }

        public void setWatches(String watches) {
            this.watches = watches;
        }

        public String getForks() {
            return forks;
        }

        public void setForks(String forks) {
            this.forks = forks;
        }

        public String getDateUpdate() {
            return dateUpdate;
        }

        public void setDateUpdate(String dateUpdate) {
            this.dateUpdate = dateUpdate;
        }

        public String getStars() {
            return stars;
        }

        public void setStars(String stars) {
            this.stars = stars;
        }

        public String getGitUrl() {
            return gitUrl;
        }

        public void setGitUrl(String gitUrl) {
            this.gitUrl = gitUrl;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFullname() {
            return fullname;
        }

        public void setFullname(String fullname) {
            this.fullname = fullname;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}

