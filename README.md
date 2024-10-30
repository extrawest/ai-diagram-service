# AI diagram creator App

[![Maintenance](https://img.shields.io/badge/Maintained%3F-yes-green.svg)]()
[![Maintainer](https://img.shields.io/static/v1?label=Yevhen%20Ruban&message=Maintainer&color=red)](mailto:yevhen.ruban@extrawest.com)
[![Ask Me Anything !](https://img.shields.io/badge/Ask%20me-anything-1abc9c.svg)]()
![GitHub license](https://img.shields.io/github/license/Naereen/StrapDown.js.svg)
![GitHub release](https://img.shields.io/badge/release-v1.0.0-blue)

This Java Spring Boot service leverages TogetherAI's advanced models to convert image and text inputs into PlantUML diagrams. It provides an API that accepts images and text descriptions as input, processes them through TogetherAI’s machine learning models, and outputs structured PlantUML code. The service facilitates quick and accurate diagram generation, particularly useful for transforming visual or textual information into clear and professional UML diagrams. This service is an ideal solution for teams needing automated, high-quality UML diagram generation from varied data inputs.



[screen-capture.webm](https://github.com/user-attachments/assets/468ef903-f220-4d69-9252-5013ba0c0499)



## Key Features

- **Image to Diagram**: Convert image into Plant diagram text using TogetherAI models.
- **Text to Diagram**: Convert text (JSON) into Plant diagram text using TogetherAI models.
- **Real-time Diagram Generation**: Outputs PlantUML-compatible code that can be rendered for visual representation, suitable for architectural, workflow, and data model diagrams.
- **REST API Architecture**: Built with Spring Boot for scalable deployment, ensuring easy integration with other services and applications.

## Tech Stack

- **Java 21**
- **SpringBoot 3.3.3**: Backend framework for building fast and scalable applications.
- **Together AI**: Provides models for image/text to diagram.
- **Langgraph4j**: A library for building stateful, multi-agents applications with LLMs, built for work with langchain4j.

## How It Works

1. **Image to Diagram**: Service is converting image into Plant diagram text using TogetherAI models.
2. **Text to Diagram**: Service is converting text (JSON) into Plant diagram text using TogetherAI models.

## Running On Local Machine (Linux):

1. Set up the following environment variables.
    - export AI_API_KEY=your_api_key;
2. Run the command: mvn exec:java -Dspring.profiles.active=local
3. Open the following link in your browser: http://localhost:8208/api/swagger-ui/index.html#/

## Contributing

Feel free to open issues or submit pull requests to improve the project. Contributions are welcome!

## License
