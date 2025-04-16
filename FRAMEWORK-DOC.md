# Java Web Framework Project Summary

This document provides an overview of the Java web framework project, detailing its structure, core components, and request handling flow based on the provided codebase snippets.

## Project Goal

The project aims to provide a lightweight MVC (Model-View-Controller) web framework for Java applications, leveraging annotations for configuration and simplifying web development tasks like routing, request handling, parameter binding, validation, and session management.

## Project Structure (Based on Packages)

The framework is organized into several packages, each responsible for a specific aspect of its functionality:

*   **`annotation`**: Contains custom annotations used throughout the framework for configuration (e.g., `@AnnotationController`, `@AnnotationURL`, `@AnnotationGetMapping`, `@AnnotationPostMapping`, `@AnnotationRequestParam`, `@AnnotationModelAttribute`, `@AnnotationRestAPI`, `@AuthController`, `@Auth`).
*   **`controller`**: Includes the main entry point of the framework, the **FrontController.java**.
*   **`engine`**: Houses the validation logic, including the **ValidationEngine.java**, **ValidationContext.java**, and **ValidationResult.java**.
*   **`exception`**: Defines custom exception types used by the framework (e.g., `BuildException`, `RequestException`, `ValidationException`).
*   **`mapping`**: Contains the **Mapping.java** class, responsible for holding URL-to-controller-method mappings and handling method invocation via reflection.
*   **`modelview`**: Defines the **ModelView.java** class, used to pass data from controllers to views.
*   **`scanner`**: Includes the **ControllerScanner.java**, which scans specified packages for controller classes and maps their annotated methods to URLs.
*   **`session`**: Provides session management capabilities (`Session.java`, `FormSession.java`).
*   **`upload`**: Contains classes for handling file uploads (`FileUpload.java`).
*   **`utils`**: Provides utility functions, like URL parsing in **Utils.java**.
*   **`validation`**: Contains validation constraint annotations (e.g., **Valid.java**, **Size.java**, `NotNull.java`).
*   **`validator`**: Includes implementations of constraint validators (e.g., `SizeValidator.java`, `NotNullValidator.java`).
*   **`verb`**: Defines classes related to HTTP verbs (`VerbAction.java`).

## Pivot Files / Core Components

Several files are central to the framework's operation:

1.  **FrontController.java**:
    *   Acts as the single entry point for all web requests (`HttpServlet`).
    *   Initializes the framework during startup ([init](#)): Reads configuration (`web.xml`), scans for controllers using [ControllerScanner](#), and builds the URL mapping table.
    *   Processes incoming requests ([processRequest](#)): Parses the URL, finds the corresponding [Mapping](#), determines the correct method based on the HTTP verb, and delegates execution.
    *   Handles responses: Differentiates between standard views ([Utils.handleModelView](#)) and REST APIs ([Utils.handleRestAPI](#)).
    *   Manages exceptions ([handleException](#)).

2.  **ControllerScanner.java**:
    *   Scans the classpath for classes annotated with `@AnnotationController` within the configured base package ([findClasses](#)).
    *   Inspects methods within controller classes for URL mapping annotations (`@AnnotationURL`) and HTTP method annotations (`@AnnotationGetMapping`, `@AnnotationPostMapping`).
    *   Populates a `HashMap<String, Mapping>` ([map](#)) where the key is the URL path and the value is a [Mapping](#) object containing the controller class name and associated method details.

3.  **Mapping.java**:
    *   Represents the link between a URL path and one or more controller methods (differentiated by HTTP verb).
    *   Stores the controller's class name ([className](#)) and a set of `VerbAction` objects ([verbActions](#)).
    *   Contains the core reflection logic ([reflectMethod](#)) to:
        *   Instantiate the controller.
        *   Inject dependencies (like [Session](#)).
        *   Check authentication/authorization ([isAccessAllowed](#)).
        *   Process method parameters using annotations (`@AnnotationRequestParam`, `@AnnotationModelAttribute`, `@AnnotationFileUpload`, [Session](#)).
        *   Convert request parameters to appropriate Java types ([convertParameterType](#)).
        *   Populate model attribute objects ([setAllModelAttribute](#)), including handling nested objects and foreign keys.
        *   Trigger validation ([ValidationEngine.validate](#)) if `@Valid` is present.
        *   Invoke the target controller method.
        *   Handle validation errors, potentially re-invoking the previous GET method for form redisplay.

4.  **ValidationEngine.java**:
    *   Provides the mechanism for validating model objects.
    *   Uses a map ([validators](#)) to associate constraint annotations (e.g., [Size](#)) with their corresponding validator implementations.
    *   The [validate](#) method iterates through fields of an object annotated with [Valid](#).
    *   For each field, it checks for constraint annotations and invokes the appropriate validator ([validateField](#)).
    *   Aggregates errors into a [ValidationResult](#) object.

5.  **ModelView.java**:
    *   A container object holding the view name (typically a JSP path) and data (`HashMap`) to be passed to the view.
    *   Provides methods to add data (`addData`) and clear data ([clearData](#)).
    *   Includes a static [dispatch](#) method (likely used in [Utils.handleModelView](#)) to set data as request attributes and forward the request to the specified view using `RequestDispatcher`.

## Request Lifecycle

1.  An HTTP request arrives at the server and is routed to the [FrontController](#) servlet.
2.  The [processRequest](#) method is invoked.
3.  The request URI is parsed using [Utils.parseURL](#) to get the application-specific path.
4.  The framework looks up the URL path in the `map` (populated during initialization) to find the corresponding [Mapping](#) object. If not found, a 404 error is generated.
5.  It checks the [Mapping](#)'s `VerbAction` set to find a method matching the request's HTTP verb (GET, POST, etc.). If no match, a 405 Method Not Allowed error is generated.
6.  The static [Mapping.reflectMethod](#) is called with the [Mapping](#), request object, and HTTP verb.
7.  Inside [reflectMethod](#):
    *   The controller class is loaded and instantiated.
    *   Session objects are created/retrieved.
    *   Authentication/Authorization checks are performed based on `@AuthController` and `@Auth` annotations.
    *   Method parameters are processed:
        *   Values for `@AnnotationRequestParam` are extracted from request parameters and converted.
        *   Objects for `@AnnotationModelAttribute` are instantiated, populated from request parameters (handling nesting and foreign keys), and potentially validated using [ValidationEngine](#) if `@Valid` is present.
        *   File uploads (`@AnnotationFileUpload`) are processed.
        *   Session objects are injected if requested.
    *   If validation fails:
        *   Errors are collected in a [ValidationResult](#).
        *   If `FormSession` is available, the previous GET request's method might be re-invoked to redisplay the form with errors and submitted data.
        *   Otherwise, a `ValidationException` is thrown.
    *   If validation passes (or wasn't required), the target controller method is invoked with the prepared arguments.
8.  The result returned by the controller method is captured.
9.  Back in [FrontController](#):
    *   It checks if the invoked method was annotated with [AnnotationRestAPI](#).
    *   If yes, [Utils.handleRestAPI](#) is called to serialize the result (or data within a [ModelView](#)) to JSON using Gson.
    *   If no, [Utils.handleModelView](#) is called. If the result is a [ModelView](#), its data is added to request attributes, and the request is forwarded to the specified view (JSP). If the result is a String, it's likely treated as a view name or directly outputted (depending on implementation details not fully shown).
10. If any exception occurs during the process, the [handleException](#) method formats an appropriate error response (HTML or JSON based on error type and request context).

## Key Features

*   **Annotation-Driven:** Configuration relies heavily on annotations (`@AnnotationController`, `@AnnotationURL`, `@AnnotationGetMapping`, `@AnnotationPostMapping`, `@AnnotationRequestParam`, `@AnnotationModelAttribute`, etc.).
*   **MVC Support:** Facilitates the Model-View-Controller pattern using [ModelView](#).
*   **RESTful API Support:** Methods annotated with [AnnotationRestAPI](#) automatically serialize return values to JSON.
*   **Automatic Parameter Binding:** Binds request parameters to method arguments (primitives, Strings, Dates, Timestamps) and populates complex objects (`@AnnotationModelAttribute`), including nested objects.
*   **Validation:** Built-in validation engine using annotations (`@Valid`, `@Size`, `@NotNull`).
*   **File Upload:** Support for single and multiple file uploads via `@AnnotationFileUpload`.
*   **Session Management:** Provides access to `HttpSession` via a custom [Session](#) wrapper and includes `FormSession` for handling form redisplay on validation errors.
*   **Authentication/Authorization:** Basic hooks for securing controllers and methods using `@AuthController` and `@Auth` annotations (relies on session attributes like `authenticated` and `profile`).
*   **Reflection-Based:** Uses Java reflection extensively for scanning, mapping, and method invocation.

## Dependencies (Implicit)

*   Java Servlet API (e.g., `javax.servlet.*`)
*   Google Gson (for JSON serialization in REST APIs)
*   `mg.jwe.orm.base.BaseModel` and `mg.jwe.orm.annotations.Id` (suggests integration with a custom ORM for foreign key handling).