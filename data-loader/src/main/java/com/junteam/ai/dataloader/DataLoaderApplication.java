package com.junteam.ai.dataloader;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication
public class DataLoaderApplication {
    private static final Logger log = LoggerFactory.getLogger(DataLoaderApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DataLoaderApplication.class, args);
    }

    @Bean
    Function<Flux<byte[]>, Flux<Document>> documentReader() {
        return resourceFlux -> resourceFlux
                .map(fileBytes -> new TikaDocumentReader(
                        new ByteArrayResource(fileBytes))
                        .get()
                        .getFirst())
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Bean
    Function<Flux<Document>, Flux<List<Document>>> splitter() {
        var splitter = new TokenTextSplitter();
        return documentFlux -> documentFlux
                .map(incoming -> splitter
                        .apply(List.of(incoming)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Value("classpath:/promptTemplates/nameOfTheCountry.st")
    Resource nameOfTheCountryTemplateResource;

    @Bean
    Function<Flux<List<Document>>, Flux<List<Document>>> titleDeterminer(ChatClient.Builder chatClientBuilder) {

        var chatClient = chatClientBuilder.build();

        return documentListFlux -> documentListFlux
                .map(documents -> {
                    if (!documents.isEmpty()) {
                        var firstDocument = documents.getFirst();

                        var countryTitle = chatClient.prompt()
                                .user(userSpec -> userSpec
                                        .text(nameOfTheCountryTemplateResource)
                                        .param("document", firstDocument.getText()))
                                .call()
                                .entity(CountryTitle.class);

                        if (Objects.requireNonNull(countryTitle).title().equals("未知")) {
                            log.warn("Unable to determine the name of a country; " +
                                    "not adding to vector store.");
                            documents = Collections.emptyList();
                            return documents;
                        }

                        log.info("Determined country title to be {}", countryTitle.title());
                        documents = documents.stream().peek(document -> {
                            document.getMetadata()
                                    .put("countryTitle", countryTitle.title());
                        }).toList();
                    }

                    return documents;
                });
    }

    @Bean
    Consumer<Flux<List<Document>>> vectorStoreConsumer(VectorStore vectorStore) {
        return documentFlux -> documentFlux
                .doOnNext(documents -> {
                    if (!documents.isEmpty()) {
                        var docCount = documents.size();
                        log.info("Writing {} documents to vector store.", docCount);

                        vectorStore.accept(documents);

                        log.info(
                                "{} documents have been written to vector store.", docCount);
                    }
                })
                .subscribe();
    }

    @Bean
    ApplicationRunner go(FunctionCatalog catalog) {
        Runnable composedFunction = catalog.lookup(null);
        return args -> {
            composedFunction.run();
        };
    }
}
