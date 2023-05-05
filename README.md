ChatGPT with Markdown
======================

ChatGPT with Markdown is a JetBrains IDE plugin to help you talk with ChatGPT from Markdown file.

# Get started

* Install plugin from IDE plugin manager
  or [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/21671-chatgpt-with-markdown)
* Create a file with `.gpt` extension, such as `demo.gpt`
* Copy following code and paste in `demo.gpt` file, and change `openai_api_key` to your own key.

~~~markdown
---
openai_api_key: xxxx
---

# Basic Java

What's Java?

# Generation JUnit 5 unit test

Please give me an example of JUnit 5 unit test.
~~~

* Click run button in the gutter to run ChatGPT query.

# Features

### Talk with ChatGPT from Markdown.

![ChatGPT Markdown File](docs/images/gpt-file.png)

### Editor split support: one for prompt, another for response.

![ChatGPT Markdown File](docs/images/gpt-file-split.png)

### `system` and `assistant` messages support, please add `{.system}` or `{.assistant}` after paragraph.

~~~markdown
# Generation unit test

You are a Java programmer, and you are developing a Spring Boot application.
The tech stack is Java 17, Spring Boot 3.0, JUnit 5 , Spring Boot Test, AssertJ etc. {.system}

Please write unit test with Spring Boot Test for the below code:

```java 
           
package org.mvnsearch.service;

import org.springframework.stereotype.Component;

@Component
public class UserService {
    public User findNickById(long id) {
        return "Jackie";
    }
}
``` 
~~~

<!-- Plugin description -->
ChatGPT with Markdown is a JetBrains IDE plugin to help you talk with ChatGPT from Markdown file.

# Get started

Create a file with `.gpt` extension, such as `demo.gpt`. Copy following code and paste in `demo.gpt` file,
and change `openai_api_key` to your own key. Click run button in the gutter to run ChatGPT query.

```markdown
---
openai_api_key: xxxx
---

# Basic Java

What's Java?
```

# Features:

* Standard Markdown format for ChatGPT: h1 is query name, and paragraph is query prompt.
* Make a talk from Markdown: Click run button in the gutter to run ChatGPT query.
* Support to load OpenAI token from environment variable `OPENAI_API_KEY`
* Editor split support: one for prompt, another for response

<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "ChatGPT with Markdown"</kbd> >
  <kbd>Install Plugin</kbd>

- Manually:

  Download the [latest release](https://github.com/linux-china/markdown-chatgpt/releases/latest) and install it manually
  using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
