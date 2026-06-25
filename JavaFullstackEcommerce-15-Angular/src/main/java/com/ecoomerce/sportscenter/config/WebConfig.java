// WebConfig.java — currently DISABLED (entire file is commented out)
//
// Purpose (when active):
//   Configures Spring MVC's Pageable argument resolver to use 1-based page numbering.
//   By default, Spring Data uses 0-based page numbers (?page=0 = first page).
//   With setOneIndexedParameters(true), ?page=1 = first page — which feels more
//   natural to end users and matches many REST API conventions.
//
// Why it is disabled:
//   The Angular frontend already sends 0-based page numbers (?page=0, ?page=1 ...),
//   matching Spring's default. Enabling this would shift the indexing and break
//   pagination in the current Angular implementation.
//
// To re-enable: remove the comment characters (//) from every line and ensure
// the Angular store component sends page numbers starting from 1 instead of 0.

//package com.ecoomerce.sportscenter.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
//import org.springframework.data.web.config.EnableSpringDataWebSupport;
//import org.springframework.web.method.support.HandlerMethodArgumentResolver;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//import java.util.List;
//
//@Configuration
//@EnableSpringDataWebSupport
//public class WebConfig implements WebMvcConfigurer {
//    @Bean
//    public PageableHandlerMethodArgumentResolver customPageableResolver(){
//        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver(){
//            @Override
//            protected String getPageParameterName(){
//                return super.getPageParameterName();
//            }
//        };
//        // This sets the default page number to 1 (1-indexed instead of the default 0-indexed)
//        resolver.setOneIndexedParameters(true);
//        return resolver;
//    }
//
//    @Override
//    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
//        resolvers.add(customPageableResolver());
//    }
//}
