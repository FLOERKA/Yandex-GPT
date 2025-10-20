# YandexGPT Java API

## Dependency

### Last version:  [![](https://jitpack.io/v/FLOERKA/Yandex-GPT.svg)](https://jitpack.io/#FLOERKA/Yandex-GPT)

Maven:
```maven
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>

	<dependency>
	    <groupId>com.github.FLOERKA</groupId>
	    <artifactId>Yandex-GPT</artifactId>
	    <version>Tag</version>
	</dependency>
```
Gradle:
```gradle
	dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url 'https://jitpack.io' }
		}
	}
	dependencies {
	        implementation 'com.github.FLOERKA:Yandex-GPT:Tag'
	}
```

## Usage

### Методы подключения:

Библиотека поддерживать возможность как создать свой объект YandexGPT, так и преобразовать его из конфигурации.

Создание своего объекта:
``` Java
    YandexGPT yandexGPT = new YandexGPT.Builder()
            .folderID("folderID")
            .oAuth("oAuth")
            .iAmToken("iamToken")
            .maxTokens(300)
            .temperature(0.3d)
            .serverPrompt("Ты - Помощник")
            .history(historyList)
            .configuration(file)
            .build();
```
Обязательные параметры: folderID, oAuth (или iAmToken), serverPrompt (или history, если тот содержит серверный промпт)
**Подключая только oAuth токен, iamToken будет создаваться автоматически перед каждым использованием!**
**Подключая configuration, вы будете получать всю историю сообщений**


Создание объекта из конфигурации:
``` Java
    Configuration configuration = new Configuration("path/to/config.yml");
    YandexGPT yandexGPT = new YandexGPT(configuration);
```
Пример конфигурации:
``` YAML
folder-id: folderID
#oauth-token: 'oAuth'
i-am-token: '12345'
max-tokens: 300
temperature: 0.3
system-prompt: Ты мой помощник
history:
  - '[role:system][message:Ты мой помощник]'
client:
  connect-timeout: 10
  read-timeout: 10
```

## Использование

synchronized:
```Java
Optional<Answer> answerOptional = yandexGPT.sendMessage("Составь мне план на вечер");
if(answerOptional.isPresent()) {
    Answer answer = answerOptional.get();
}
```

async version:
```Java
CompletableFuture<Answer> answerFuture = yandexGPT.sendMessageAsync("Составь мне план на вечер");
answerFuture.thenAccept(answer -> {
    // usage
});
```
