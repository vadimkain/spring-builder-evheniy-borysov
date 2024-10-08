# Евгений Борисов — Spring-построитель
[Ссылка на доклад](https://youtu.be/rd6wxPzXQvo?si=8JC6SRRf-TIick5D)

Будем писать корона-дезинфектор, но в процессе написания этой задачи вместо того чтобы написать красивый код возникнет необходимость написасть несекую инфраструктуру которая позволит не копи-пейстить общую логику которая сделает наш код гибким, которая будет соблюдать все правила SOLID и в процессе написания и развития этой инфрастркутуры которая будет подгоняться под нашу бизнес-задачу - у нас появится некий фреймворк который будет аналогом спринга. Т.е. там не будет BeanPostProceesor/BeanFactorPostProcessor не будет, но будут очень аналогичные похожие вещи. Т.е. будем смотреть на то как работает спринг как его создатель.

У автора доклада 11 java и он хочет пользоваться всякими аннотациями которые вынесли из java core и засунули всякие отдельные jsr'і - у нас есть отдельная jsr которая добавляет `@PostConstruct`
```xml
        <dependency>
            <groupId>javax.annotation</groupId>
            <artifactId>jsr250-api</artifactId>
            <version>1.0</version>
        </dependency>
```

Также подключаем библиотеку хорошего друга автора которая расширяет возможности встроенного Reflection API.
```xml
        <dependency>
            <groupId>org.reflections</groupId>
            <artifactId>reflections</artifactId>
            <version>0.9.12</version>
        </dependency>
```

## Начинаем. 
И так, у нас есть комната которую будем дезинфецировать
```java
package com.example;

public class Room {
}
```

и класс `CoronaDesinfetor`

```java
package com.example;

public class CoronaDesinfector {
    public void start(Room room) {
        desinfect(room);
    }

    private void desinfect(Room room){
        System.out.println("зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад");
    }
}
```

И так, что нам надо сделать в методе старт:
1. Сообщить всем присутствующим в комнате о начале дезинфекции и попросить всех уйти
2. Разогнать всех кто не вышел после объявления
3. Провести дезинфекцию (`desinfect(room)`)
4. Сообщить всем присутвующим в комнате, что они могут вернуться обратно

Как мы будем это делать. В принципе, мы могли бы сразу писать логику внутри метода, но мы же знаем что такое Single Responsibility, что такое Open-Closable принцип и всякие другие хорошие вещи. Соответственно мы будем **инкапсулировать** логику которая используется в других местах. Т.е. например логичнее было бы использовать какой-то объект который будет называться `Announcer` у которого будет метод `announce` и будет говорить "начинаем дезинфекцию".

Соответственно что нам это даёт, почему мы начинаем методы инкапсулировать в отдельные объекты а не прописывать приватными методами как метод `desinfect()` - потому что мы за Single Responsibility. Если нам завтра понадобится какой-то другой сервис, который будет делать какую-ту другую логику, но при этом тоже захочет припомощи `Announcer` выводить сообщения, то он сможет воспользоваться тем же самим объектом.

```java
package com.example;

public class CoronaDesinfector {
    public void start(Room room) {
        announcer.announce("Начинаем дезинфекцию, всё вон!");
        desinfect(room);
    }

    private void desinfect(Room room){
        System.out.println("зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад");
    }
}
```

Почему нам это всё важно? Поговорим про SOLID в двух словах. Почему важно соблюдать пять основных правил два из которых опускаем потому что они наименее интересны

- S - Single Responsibility
- O - Open Close Principle
- L
- I
- D - Dependency Inversion

В Java одна из центральных механизмов рантайма это ClassLoader который подгружает класс. Когда он подгружает классы - он не умеет относится к версиями jar'ов. Т.е. он смотрит по пакетам какой класс ему нужно загрузить и если теоретически в один classpath попало 2 jar с разными версиями, то он загрузит все равно первый попавшийся класс потому что он не умеет относиться к версиям. Это потому что когда придумали джаву не было такого понятия как "версия"

Современный мир который очень динамичен, в котором есть Content Delivery, в котором никакая версия продукта не бывает конечной потому что постоянно динамично мир развивается и появляются новые фичи - у нас появляются новые правила кодописания. Наш код должен быть достаточно гибкий, чтобы можно было заменять всякие вещи, заменять приложение под нового клиента которому нужны некоторые другие имплементации сервисов и при этом нам не надо будет ковыряться в коде, переписывать кучу класов и т.ж.

Правила кодаписания:
- Flexible - код должен быть гибким
- Reusable - код должен быть переиспользованным. Т.е. если написали полезный, замечательный класс `Announcer` при помощи которого можно делать `announcment`, то его должны использовать всякие другие сервисы. Если у нас есть класс который умеет разгонять людей из комнаты, то опять же логично что не только `CoronaDesinfector` будет пользоваться этим сервисом. Достаточно полезная функция "выгонять людей из комнаты" и соответственно мы эту функцию инкапсулируем в объект. Таким образом мы достигаем того что наш код Reusable
- Readable - код должен быть читабельным для того чтобы его можно было легко поддерживать и исправлять.

Теперь разгоним всех, кто не вышел после объявления (2 операция). Это будет делать наш полицейский `Policeman` у которого будет метод `makePeopleLeaveRoom()`. И в конце нам надо сообщить что можно заходить обратно и соответственно берем для этого `announcer.announce()` (действие 4).

```java
package com.example;

public class CoronaDesinfector {
    public void start(Room room) {
        announcer.announce("Начинаем дезинфекцию, всё вон!");
        policeman.makePeopleLeaveRoom();
        desinfect(room);
        announcer.announce("Рискните зайти обратно");
    }

    private void desinfect(Room room){
        System.out.println("зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад");
    }
}
```

Заметим, что Single Responsibility класса `CoronaDesinfector` исключительно в имплементации метода `desinfect(Room room)`.

Создадим теперь объекты которые будем в дальнейшем инжектить.

```java
package com.example;

public class CoronaDesinfector {

    private Announcer announcer = new ConsoleAnnouncer();
    private Policeman policeman = new PolicemanImpl();

    public void start(Room room) {
        announcer.announce("Начинаем дезинфекцию, всё вон!");
        policeman.makePeopleLeaveRoom();
        desinfect(room);
        announcer.announce("Рискните зайти обратно");
    }

    private void desinfect(Room room) {
        System.out.println("зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад");
    }
}
```

`Announcer` имеет смысл делать интерфейсом как и большинство наших сервисов потому что возможно для разных клиентов `Announcer` может быть имплементирован по разному и если мы привяжемся к конкретной имплементации, то проблемы будут у разработчика.

```java
package com.example;

public interface Announcer {
    void announce(String message);
}

```
```java
package com.example;

public class ConsoleAnnouncer implements Announcer {
    @Override
    public void announce(String message) {
        System.out.println(message);
    }
}

```
```java
package com.example;

public interface Policeman {
    void makePeopleLeaveRoom();
}

```
```java
package com.example;

public class PolicemanImpl implements Policeman {
    @Override
    public void makePeopleLeaveRoom() {
        System.out.println("пиф паф, бах бах, кыш, кыш!");
    }
}
```

И так, если мы запустим то все выведится нормально. Давайте посмотрим на этот класс и посчитаем сколько у этого класса есть Responsibilities. До этого мы говорили что единственный респонсибилити это его метод `desinfect()`, а остальное это просто делегация в другие существующие объекты.
```java
package com.example;

public class CoronaDesinfector {

    private Announcer announcer = new ConsoleAnnouncer();
    private Policeman policeman = new PolicemanImpl();

    public void start(Room room) {
        announcer.announce("Начинаем дезинфекцию, всё вон!");
        policeman.makePeopleLeaveRoom();
        desinfect(room);
        announcer.announce("Рискните зайти обратно");
    }

    private void desinfect(Room room) {
        System.out.println("зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад");
    }
}
```

Тем не менее правильно ли сказать что у него только одна респонсибилити? Здесь от 7 до бесконечности респонсибилити у класса `CoronaDesinfector`. 

Вернёмся к тому что кроме метода `start()` и `desinfect()` у нас есть два филда `announcer` и `policeman` которые создаются при помощи `CoronaDesinfector`. Т.е. `Announcer` это интерфейс и мы сказали что у нас в будущем возможно будет множество имплементаций этого `announcer`. Кто на данный момент принимает решение, что именно `ConsoleAnnouncer` должен быть использован для данного приложения? Это решение принимает `CoronaDesinfector`, т.е. у него есть ещё один респонсибилити. Сейчас это решение тупо захардкожено и мы привязаны к конкретной имплементации. 

Если завтра нам понадобится поменять, то придется вскрывать код `CoronaDesinfector` и куча других объектов которые пользуются `ConsoleAnnouncer` и заменять там на другую имплементацию что уже не хорошо. Но если у нас появятся несколько имплементаций, какая-то сложная хитрая логика на основании которой я должен выбрать нужную имплементацию, то весь код который будет выбирать имплементацию на основании каких-то конвенций/конфигураций и принципов будет находится в `CoronaDesinfector` и `CoronaDesinfector` начинает обрастать дополнительным кодом который не имеет никакого отношения к дезинфекции комнат, а имеет отношение к тому как надо правильно выбирать ... не знаю там сходить в env variables, посмотреть локали, посмотреть в какой стране находится компьютер на котором поднимают приложение и в зависимости от этого выбрать нужную имплементацию которая говорит нам на нужном языке с нужной терминологией по каким-то правилам этой страны.

На данный момент видим две респонсибилити: `desinfect()` и  `new ConsoleAnnouncer()`.

Дополнительные респонсибилити нашего `CoronaDesinfector` заключаются в том что он должен уметь и очень хорошо понимать как именно создавать `ConsoleAnnouncer`. Сегодня чтобы создать `ConsoleAnnouncer` надо воспользоваться простым инструктором, но завтра автор класса `ConsoleAnnouncer` может решить и переписать его на какой-то домашний Singletone или воспользоваться дизайн-паттерном builder и соответственно о бо всем этом должен знать наш `CoronaDesinfector` потому что он же отвечает за создание данных имплементаций. И если эта имплементация `ConsoleAnnouncer` опять поменяется, то нам придется заходить в код `CoronaDesinfector` и всех других объектов которые создают для себя этот `ConsoleAnnouncer`.

Третья последняя вещь которую должен делать наш `CoronaDesinfector` против нашего `ConsoleAnnouncer` - он должен уметь его настроить. На данный момент для того чтобы пользоваться `ConsoleAnnouncer`, для того чтобы его настроить надо сделать ничего. Поэтому это не заметно. Но если автор `ConsoleAnnouncer` напихает в имплементацию куча разных пропертей и решит что эти все проперти надо настраивать при помощи сеттеров, то таким образом получится что вся эта респонсибилити ложится на того кто создаёт этот `ConsoleAnnouncer` потому что его недостаточно просто создать, его надо ещё настроить.

**Подведём промежуточные итоги.**

Наш `CoronaDesinfector` умеет выбрать правильную имплементацию `Announcer`, он умеет создать создать `ConsoleAnnouncer` и он умеет его правильно настроить. Тоже самое про `Policeman` - он умеет выбрать правильную имплементацию, умеет её создать и умеет настроить. Таким образом у нас уже получается по три респонсибилити в
```java
    private Announcer announcer = new ConsoleAnnouncer();     
    private Policeman policeman = new PolicemanImpl();        
```
Почему до бесконечности? Потому что завтра если у нас появятся другие имплементации этого `Announcer`, то он соответственно точно также должен уметь, знать и понимать как они должны создаваться и как они должны настраиваться. Чем больше будет имплементаций, тем больше будет становиться класс `CoronaDesinfector` потому что у него нахрен никакой не сингл респонсибилити.

**А кто будет решать какая имплементация?**

Когда появилась джава в 1995 году - люди по началу не очень понимали принцип ООП потому что опыт который был накоплен - более процедуральный опыт и писали на статических методах всё. Потом в конце 90х, начало 2000х люди отлично поняли что джава не для статических методов, а для объектов. Объекты можно юзать, создавать, настраивать, передавть как параметры, инкапсулировать логику и т.д. Люди начали создавтаь объекты при помощи new. И не было опять же никакой проблемы с тем чтобы люди для себя каждый кто хочет какой-то объект создать сам его создавал, сам его настраивал. Если другому классу нужен такой объект также настроенный то не вопрос - скопипейстим. Люди не понимали что это плохо, почему - потому что не было версионированности, не было Content Delivery. Когда люди поняли что написать код намного менее важно чем сделать его таким чтобы его можно было потом развивать. 

Отсюда эта необходимость и как начинается сейчас проект - сели архитекторы, продумали инфраструктуру, какие фреймворки берём и не начинают писать бизнес-логику. Надо сначала какие-то метрики оценить, убедиться что наша инфраструктура позволит продукту развиваться быстро, хорошо и конкурентноспособно. 

Соответственно как только люди поняли - появился очень модный и популярный дизайн-паттерн Factory. И на каждый чих и пых писали свою фабрику. Т.е. хотим создавать `Announcer`'ы - у нас для этого есть `AnnouncerFactory`, для `Policeman` фабрика `PolicemanFactory`, а если нужно получить фабрику, то у нас для этого есть `FactoryFactory`. Короче очень популярный в начале 2000х годов был мем: *"Хотел чувак использовать джаву и теперь у него одна большая сплошная ProblemFactory"* потому что на каждый чих и пых своя фабрика которые тоже надо настраивать и получается что мы сместили говнокод с одного места на другое место.

После этого ООП перешло в следующий этап эволюции и во многих проектах стали писать инфраструктуру в которой ядром являлся какой-то класс который назывался `ObjectFactory`. 

Были времена когда студент приходил после университета и какой-то бедной практики на проект реальный и перед тем как писать код ему начальник сразу бил по пальцам ещё до того как строчку написал говоря: *"так послушай чувак, ты сейчас наверняка собираешься создавать объекты через new. Вот смотрим, если ты создаёшь какого-то Person, то не вопрос. Можешьь создавать его через new хотя вряд ли. Создаешь персона через new, выкачиваешь из базы и заполняешь данными. Но если ты создаешь сервисы которые кстати по большинству своем будут синглтонами у которых есть интерфейсы и имплементация может изменяться, то ни в коем случае не создавай их через new иначе тебе придется их настраивать. У нас есть замечательных класс который называется `ObjectFactory` - это синглтон, он доступен с любой точки и каждый раз когда тебе нужен объект ты говоришь: ObjectFactory иди сюда, создай мне объект такого-то типа.`"* 

Это ещё далеко не инверсия контроля, это ещё не спринг, но давайте напишем этот `ObjectFactory`.

Создаём домашний синглтон (тогда люди ещё не понимали что синглтон это антипаттерн, но поговорим позже про это). И соответственно в нашем `ObjectFactory` будет метод который будет создавать и возвращать объект (в спринге этот метод называет `getBean()`).

```java
package com.example;  
  
public class ObjectFactory {  
    private static ObjectFactory ourInstance = new ObjectFactory();  
  
    public static ObjectFactory getInstance() {  
        return ourInstance;  
    }  
  
    private ObjectFactory() {  
  
    }  
  
    public <T> T createObject(Class<T> type) {  
          
    }  
  
}
```

Наш `CoronaDesinfector` теперь не будет создавть объекты через new, а будет ходить в нашу фабрику и просить создать объект передавая интерфейс. Уже намного лучше, уже намного красивее, но это ещё не конец и это тоже плохо. Но в 2003 это считали просто замечательно и шикарно.

```java
public class CoronaDesinfector {  
  
    private Announcer announcer = ObjectFactory.getInstance().createObject(Announcer.class);  
    private Policeman policeman = ObjectFactory.getInstance().createObject(Policeman.class);  
  
    public void start(Room room) {  
        announcer.announce("Начинаем дезинфекцию, всё вон!");  
        policeman.makePeopleLeaveRoom();  
        desinfect(room);  
        announcer.announce("Рискните зайти обратно");  
    }  
  
    private void desinfect(Room room) {  
        System.out.println("зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад");  
    }  
}
```

У нас `CoronaDesinfector` получается вообще ничего не знает ни про `Announcer`, ни про `Policeman`. Он знает, что они ему нужны, но это интерфейс и он не знает какая имплементация, не знает как её настроить, не знает какой конкретно класс будет реализован. Об этом заботится фабрика.

Но в большинстве случаем в `createObject` будет прилетать не конкретный класс, а может быть интерфейс. Поэтому мы должны в этом методе сделать проверочку

```java
package com.example;  
  
public class ObjectFactory {  
    private static ObjectFactory ourInstance = new ObjectFactory();  
  
    public static ObjectFactory getInstance() {  
        return ourInstance;  
    }  
  
    private ObjectFactory() {  
  
    }  
  
//    Аналог getBean в спринге  
    public <T> T createObject(Class<T> type) {  
//        сначала будет равен type потому что а вдруг это гласс конкретный?  
        Class<? extends T> implClass = type;  
//        но делаем проверку если тип является интерфейсом, то тогда этот тип нужно заменить  
        if (implClass.isInterface()) {  
            implClass =   
        }  
    }  
  
}
```

на `implClass =` вопрос - а у кого спросим эту имплементацию? Мы конечно можем написать какой-то код внутри своей фабрики которая будет вычитывать какие-то конфигурации, сканировать пакеты, искать сколько есть вариантов для имплементаций, но опять же мы за Single Responsibility и это неправильно что наша фабрика отвечала за всю работу с конфигурациями. Фабриками имеет метод `createObject` и она должна уметь создавать объекты, а не уметь вычитывать какие-то конфигурационные настройки. 

Поэтому у нас будет замечательный класс `config` с методом `getImplClass()`.  Сделаем также `Config` интерфейсом потому что сегодня у нас будет один конфиг который будет работать с джавовским кодом, завтра будет конфиг который будет с гугл скрипта вычитывать конфигурации, послезавтра будет конфиг который будет из JSON или XML вычитывать и т.д.

```java
package com.example;  
  
public interface Config {  
    <T> Class<? extends T> getImplClass(Class<T> ifc);  
}
```

Конфиг должен уметь получить интерфейс, а возвращать имплементацию. Сразу сделаем реализацию конфига

```java
package com.example;  
  
public class JavaConfig implements Config {  
    @Override  
    public <T> Class<? extends T> getImplClass(Class<T> ifc) {  
        return null;  
    }  
}
```

Что сделаем здесь. Давайте скажем следующее: в большинстве случаем имплементация интерфейсов будет одна. Возможно у разных клиентов она будет не такая как у другого клиента, но скорее всего при сборке у нас будет какой-то .jar в которой будет лежать имплементация и имплементация для каждого интерфейса чаще всего будет одна. Идея в следующем: я пишу какое-то приложение, которым будут пользоваться разные клиенты. На этапе сборки мы можем подложить .jar в котором лежат имплементации которые написал клиент для себя сам. Ну или клиент взял приложения у меня и сам дописывает/кастомизирует его под себя, дописывает разные имплементации, кладёт какой-то .jar, добавляет в зависимости и когда все собирается  - имплементация всегда будет одна.

Давайте будем вначале исходить от этого предположения. Потом это предположение нарушим и посмотрим как наш `getImplClass` поменяется, но пока допустим у нас только одна имплементация одновременно в classpath может находиться.

Нам понадобится класс `Reflections` который расширяет возможности существующего `Reflection` в java.  Этот `reflection` строится через создание объекта `new Reflections()`. В него можно передать пакет который надо просканировать и потом этот сканер сможет отвечать на вопросы против этого пакета. Этот пакет будем принимать в конструкторе `JavaConfig`. 

```java
package com.example;  
  
import org.reflections.Reflections;  
  
public class JavaConfig implements Config {  
    private Reflections scanner;  
  
    public JavaConfig(String packageToScan) {  
        this.scanner = new Reflections(packageToScan);  
    }  
  
    @Override  
    public <T> Class<? extends T> getImplClass(Class<T> ifc) {  
        return null;  
    }  
}
```

Теперь каждый раз когда обращаемся в `getImplClass` - мы можем сделать следующее:

```java
package com.example;  
  
import org.reflections.Reflections;  
  
import java.util.Set;  
  
public class JavaConfig implements Config {  
    private Reflections scanner;  
  
    public JavaConfig(String packageToScan) {  
        this.scanner = new Reflections(packageToScan);  
    }  
  
    @Override  
    public <T> Class<? extends T> getImplClass(Class<T> ifc) {  
  
//        просим у сканнера все подвиды данного интерфейса.  
//        другими словами получаем все классы которые реализуют данный интерфейс  
        Set<Class<? extends T>> classes = scanner.getSubTypesOf(ifc);  
//        если у нас размер не является 1 (либо есть импл или их больше 1, то продолжать не можем)  
        if (classes.size() != 1) {  
            throw new RuntimeException(ifc + " has 0 or more than one implementation");  
        }  
  
//        Берём ту единственную имплементацию которую нашли  
        return classes.iterator().next();  
    }  
}
```

Возвращаемся в `ObjectFactory` и настраиваем этот `Config`, но пока захардкодим. После того как проскочили `if (type.isInterface)` и абсолютно точно знаем что `implClass` содержит уже конкретную реализацию которая либо была изначально потому что сюда могли передать конкретный класс в `createObject(Class<T> type)`, либо мы её получили из конфига `config.getImplClass(type)` и теперь мы можем соответственно взять этот имплементированный класс. Соответственно мы накладываем конвенцию что у нас должен быть дефолтный конструктор, у которого вызываем `.newInstance()`:

```java
package com.example;  
  
import lombok.SneakyThrows;  
  
public class ObjectFactory {  
    private static ObjectFactory ourInstance = new ObjectFactory();  
    private Config config = new JavaConfig("com.example");  
  
    public static ObjectFactory getInstance() {  
        return ourInstance;  
    }  
  
    private ObjectFactory() {  
  
    }  
  
    @SneakyThrows  
    //    Аналог getBean в спринге  
    public <T> T createObject(Class<T> type) {  
//        сначала будет равен type потому что а вдруг это гласс конкретный?  
        Class<? extends T> implClass = type;  
//        но делаем проверку если тип является интерфейсом, то тогда этот тип нужно заменить  
        if (implClass.isInterface()) {  
            implClass = config.getImplClass(type);  
        }  
  
        return implClass.getDeclaredConstructor().newInstance();  
    }  
  
}
```

**Посмотрим что произойдёт если кто-то из клиентов напишет другую имплементацию полицейского**.

Можно предположить что те имплементации которые не являются общими между разными клиентами автор вообще не пишет и даёт им самим имплементировать. 

Допустим мы считаем что полицейский абсолютно одинаковый для всех, а вот какой-то клиент захотел своего `Policement` подложить, но при этом выбросить `PolicemanImpl` он не может потому что она находится в jar которая допустим является core. Что произойдёт?

```java
package com.example;  
  
public class AngryPoliceman implements Policeman {  
    @Override  
    public void makePeopleLeaveRoom() {  
        System.out.println("Всех убью! Вон пошли");  
    }  
}
```

```java
package com.example;  
  
public class App   
{  
    public static void main( String[] args )  
    {  
        CoronaDesinfector coronaDesinfector = new CoronaDesinfector();  
        coronaDesinfector.start(new Room());  
    }  
}
```

Запустим и увидим ошибку которую мы предвидели ранее в `JavaConfig`

```txt
Exception in thread "main" java.lang.RuntimeException: interface com.example.Policeman has 0 or more than one implementation
	at com.example.JavaConfig.getImplClass(JavaConfig.java:22)
	at com.example.ObjectFactory.createObject(ObjectFactory.java:24)
	at com.example.CoronaDesinfector.<init>(CoronaDesinfector.java:6)
	at com.example.App.main(App.java:11)

Process finished with exit code 1
```

Мы же хотим все таки дать возможность клиентам решать какие имплементации использовать. Мы не хотим чтобы они пинцетом ковырялись, открывали .jar, писали какие-то плагины для билда которые будут удалять какие-то классы, чтобы не было дупликатов. 

Мы хотим сделать гибко. Поэтому можем взять наш `JavaConfig` и сказать что этот конфиг кроме сканера при помощи которого он будет сканировать все пакеты - он ещё будет иметь некую `Map<Class, Class>` (можно позже сделать мульт-мапу если хотим поддерживать множественную имплементацию) которая будет называться `ifc2ImplClass`. Эту мапу также будем принимать в конструкторе `JacaConfig`.

```java
package com.example;  
  
import org.reflections.Reflections;  
  
import java.util.Map;  
import java.util.Set;  
  
public class JavaConfig implements Config {  
    private Reflections scanner;  
    private Map<Class, Class> ifc2ImplClass;  
  
    public JavaConfig(String packageToScan, Map<Class, Class> ifc2ImplClass) {  
        this.scanner = new Reflections(packageToScan);  
        this.ifc2ImplClass = ifc2ImplClass;  
    }  
  
    @Override  
    public <T> Class<? extends T> getImplClass(Class<T> ifc) {  
  
//        просим у сканнера все подвиды данного интерфейса.  
//        другими словами получаем все классы которые реализуют данный интерфейс  
        Set<Class<? extends T>> classes = scanner.getSubTypesOf(ifc);  
//        если у нас размер не является 1 (либо есть импл или их больше 1, то продолжать не можем)  
        if (classes.size() != 1) {  
            throw new RuntimeException(ifc + " has 0 or more than one implementation");  
        }  
  
//        Берём ту единственную имплементацию которую нашли  
        return classes.iterator().next();  
    }  
}
```

Теперь мы можем не бежать сразу сканировать пакеты, а сначала спросить может наш `JavaConfig` уже знает про это в `Map<Class, Class> ifc2ImplClass`.

```java
package com.example;  
  
import org.reflections.Reflections;  
  
import java.util.Map;  
import java.util.Set;  
  
public class JavaConfig implements Config {  
    private Reflections scanner;  
    private Map<Class, Class> ifc2ImplClass;  
  
    public JavaConfig(String packageToScan, Map<Class, Class> ifc2ImplClass) {  
        this.scanner = new Reflections(packageToScan);  
        this.ifc2ImplClass = ifc2ImplClass;  
    }  
  
    @Override  
    public <T> Class<? extends T> getImplClass(Class<T> ifc) {  
  
//        если наш JavaConfig не знает про имплементацию для интерфейса ничего  
        return ifc2ImplClass.computeIfAbsent(ifc, aClass -> {  
  
//        просим у сканнера все подвиды данного интерфейса.  
//        другими словами получаем все классы которые реализуют данный интерфейс  
            Set<Class<? extends T>> classes = scanner.getSubTypesOf(ifc);  
//        если у нас размер не является 1 (либо есть импл или их больше 1, то продолжать не можем)  
            if (classes.size() != 1) {  
                throw new RuntimeException(ifc + " has 0 or more than one implementation");  
            }  
  
//        Берём ту единственную имплементацию которую нашли  
            return classes.iterator().next();  
        });  
    }  
}
```

`computeIfAbsent` принимает ключ и если ключ существует, то возвращает значение для этого ключа и всё. Если этот ключ не существует, то запускает лямбду. Лямбда в конечном итоге возвращает значение. Это значение в `return classes.iterator().next()` засетится в мэпу на будущее и за одно вернется к нам.

Таким образом убиваем двух зайцев потому что процесс сканирования трудоемкий. Если второй раз у нас просят `Announcer` который уже один раз попросили и мы уже один раз просканировали и мы уже знаем что имплементация `Announcer` это `ConsoleAnnouncer` - второй раз уже не нужно сканировать и искать потому что сохранили в нашу мепу.

Возвращаемся в `ObjectFactory`. Теперь `Config` инициализируем в конструкторе, тоже захардкодим:

```java
package com.example;  
  
import lombok.SneakyThrows;  
  
import java.util.HashMap;  
import java.util.Map;  
  
public class ObjectFactory {  
    private static ObjectFactory ourInstance = new ObjectFactory();  
    private Config config;  
  
    public static ObjectFactory getInstance() {  
        return ourInstance;  
    }  
  
    private ObjectFactory() {  
        this.config = new JavaConfig("com.example", new HashMap<>(Map.of(Policeman.class, AngryPoliceman.class)));  
    }  
  
    @SneakyThrows  
    //    Аналог getBean в спринге  
    public <T> T createObject(Class<T> type) {  
//        сначала будет равен type потому что а вдруг это гласс конкретный?  
        Class<? extends T> implClass = type;  
//        но делаем проверку если тип является интерфейсом, то тогда этот тип нужно заменить  
        if (implClass.isInterface()) {  
            implClass = config.getImplClass(type);  
        }  
  
        return implClass.getDeclaredConstructor().newInstance();  
    }  
}
```

`HashMap` из `new JavaConfig("com.example", new HashMap<>(Map.of(Policeman.class, AngryPoliceman.class)));  ` может строиться из какого-нибудь внешнего конфигурационного файла. Может быть на питоне есть какой-то файл или XML есть какой-то который можем просканировать в рантайме и построить из информации которая в текстовом режиме написана в эту `HashMap`. При чем завтра если захотим поменять имплементацию, то даже не придется чинить код. Просто конфигурационный файл подправим из которого строится мэпа и соответственно все будет замечательно.

Запустим и проверим что все работает теперь:

```txt
Начинаем дезинфекцию, всё вон!
Всех убью! Вон пошли
зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад
Рискните зайти обратно

Process finished with exit code 0

```

**Подведём итоги. Что нам это дало?**

*Централизованное место создания всех объектов.* 

 *Если надо менять имплементацию не надо лезть в код (Гибкость)*. Напоминание, что изменения в `HashMap` из `new JavaConfig("com.example", new HashMap<>(Map.of(Policeman.class, AngryPoliceman.class)));` это не изменение в коде. Это изменение в конфигурации которая сейчас находится внутри `ObjectFactory`, но в будущем понятно что можно будет вынести куда-то наружу и соответсвенно человек который будет создавать фабрику - он сможет передать конфигурационный файл который будет считан в runtime и соответственно мэпа построится в рантайме.

*Перед тем как фабрика отдаст объект, она его может настроить согласно нашим конвенциям, которые мы придумаем в будущем.* Делать какие-то injection'ы, проксировать классы, транзакции, АОП и т.д.

```java
    @SneakyThrows  
    //    Аналог getBean в спринге  
    public <T> T createObject(Class<T> type) {  
//        сначала будет равен type потому что а вдруг это гласс конкретный?  
        Class<? extends T> implClass = type;  
//        но делаем проверку если тип является интерфейсом, то тогда этот тип нужно заменить  
        if (implClass.isInterface()) {  
            implClass = config.getImplClass(type);  
        }  
  
        T t = implClass.getDeclaredConstructor().newInstance();  
  
//        todo: Вся магия по настройка объекта тут  
  
        return t;  
    }
```

Для того чтобы продемонстрировать пример настройки давайте сделаем следующее: мы хотим чтобы наш `ConsoleAnnouncer` выводил рекламу каждый раз когда он выводил сообщение. 

У него будет какой-то рекомендатор, это будет опять же интерфейс.

```java
package com.example;  
  
public class ConsoleAnnouncer implements Announcer {  
  
    private Recommendator recommendator = ObjectFactory.getInstance().createObject(Recommendator.class);  
  
    @Override  
    public void announce(String message) {  
        System.out.println(message);  
    }  
}
```

```java
package com.example;  
  
public interface Recommendator {  
    void recommend();  
}
```

Напишем простую реализацию нашего рекомендатора который будет печатать то что проплатила компания. Т.е. тот клиент у которого приложение поднято - у него в каком-то пропертис файле может лежать информация о том какой drink мы рекламируем.

Алкоголь будет филдом который мы захотим получать из какого-то проперти файла. Делаем для этого аннотацию.

```java
package com.example;  
  
public class RecommendatorImpl implements Recommendator {  
    @InjectProperty  
    private String alcohol;  
  
    @Override  
    public void recommend() {  
        System.out.println("to protect from covid-2019 drink " + alcohol);  
    }  
}
```

Сделаем так чтобы можно было указывать либо имя проперти которого надо инжектить, либо можем если имя не указал чтобы название филда было названием проперти которого надо инжектить.

```java
package com.example;  
  
import java.lang.annotation.Retention;  
import java.lang.annotation.RetentionPolicy;  
  
@Retention(RetentionPolicy.RUNTIME)  
public @interface InjectProperty {  
    String value() default "";  
}
```

Создаём пропертис файл по пути `src/main/java/resources/application.properties`

```txt
alcohol=beer corona
```

Если вернёмся в наш `ConsoleAnnouncer`, то он соответственно будет брать этот рекоммендатор и говорить ему: "давай рекомендуй"

```java
package com.example;  
  
public class ConsoleAnnouncer implements Announcer {  
  
    private Recommendator recommendator = ObjectFactory.getInstance().createObject(Recommendator.class);  
  
    @Override  
    public void announce(String message) {  
        System.out.println(message);  
        recommendator.recommend();  
    }  
}
```

и запустим то увидим drink null:

```txt
Начинаем дезинфекцию, всё вон!
to protect from covid-2019 drink null
Всех убью! Вон пошли
зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад
Рискните зайти обратно
to protect from covid-2019 drink null

Process finished with exit code 0
```

Что мы сейчас хотим сделать: мы хотим нашу фабрику научить перед тем как она вернёт объект - обучить как этот объект можно настроить. Т.е. мы создаём объект, но перед тем как вернём хотим посмотреть, а может у него есть какие-то филды в которые надо заинжектить какие-то пропертисы.

Берём наш `implClass`, получаем у него все его филды (используем `getDeclaredFields` поскольку не делаем продукт на продажу и по хорошему надо не `DeclaredFields` брать, а если хотим поддерживать филды которые пришли от родителей, то придется писать рекурсию или пользоваться Reflection Utils, но не принципиально). Берём все филды данного класса, итерируемся. У каждого филда пытаемся считать аннотацию которая называется `InjectProperty.class`. Если аннотация есть, то если значение в аннотации пустое, то тогда мы захотим вытаскивать из нашего пропертис файла по названию филда. Пока будем писать всё некрасиво.

Надо написать код который будет вытаскивать из `application.properties` информацию. Один из способов написания кода - сначала писать всё в кучу пока мысль не пропала, а потом обязательно рефактор чтобы разнести всё по классам.

Пока скажем следующее: мы хотим просканировать, построить `Map`'y из наших проперти файлом. Берём `ClassLoader`, говорим ему давай притащи ресурс который называется `application.properties` (в будущем можем передавать информацию не только какие пакеты сканировать, но и какие дополнительные проперти файлы подтянуть кроме этого).

```java
package com.example;  
  
import lombok.SneakyThrows;  
  
import java.io.BufferedReader;  
import java.io.InputStream;  
import java.io.InputStreamReader;  
import java.lang.reflect.Field;  
import java.util.HashMap;  
import java.util.Map;  
import java.util.stream.Stream;  
  
import static java.util.stream.Collectors.toMap;  
  
public class ObjectFactory {  
    private static ObjectFactory ourInstance = new ObjectFactory();  
    private Config config;  
  
    public static ObjectFactory getInstance() {  
        return ourInstance;  
    }  
  
    private ObjectFactory() {  
        this.config = new JavaConfig("com.example", new HashMap<>(Map.of(Policeman.class, AngryPoliceman.class)));  
    }  
  
    @SneakyThrows  
    //    Аналог getBean в спринге  
    public <T> T createObject(Class<T> type) {  
//        сначала будет равен type потому что а вдруг это гласс конкретный?  
        Class<? extends T> implClass = type;  
//        но делаем проверку если тип является интерфейсом, то тогда этот тип нужно заменить  
        if (implClass.isInterface()) {  
            implClass = config.getImplClass(type);  
        }  
  
        T t = implClass.getDeclaredConstructor().newInstance();  
  
        for (Field field : implClass.getDeclaredFields()) {  
            InjectProperty annotation = field.getAnnotation(InjectProperty.class);  
  
            InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("application.properties");  
//            String path = ClassLoader.getSystemClassLoader().getResource("application.properties").getPath();  
  
            Stream<String> lines = new BufferedReader(new InputStreamReader(inputStream)).lines();  
//            Stream<String> lines = new BufferedReader(new FileReader(path)).lines();  
            Map<String, String> propertiesMap = lines.map(line -> line.split("=")).collect(toMap(arr -> arr[0], arr -> arr[1]));  
  
            if (annotation != null) {  
                String value;  
                if (annotation.value().isEmpty()) {  
                    value = propertiesMap.get(field.getName());  
                } else {  
                    value = propertiesMap.get(annotation.value());  
                }  
  
                field.setAccessible(true);  
//                настраиваем объект t  
                field.set(t, value);  
            }  
        }  
  
        return t;  
    }  
}
```

Т.е. если аннотация у нас была пустая, то тогда берём имя филда и оно является названием проперти, а если значение было, то берём его. Таким образом из этой `Map` вытаскиваем значение которое нам нужно засетить в филд. 

Запустим и проверим что у нас будет уже не `drink null`, а `drink beer`:

```txt
Начинаем дезинфекцию, всё вон!
to protect from covid-2019 drink beer corona
Всех убью! Вон пошли
зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад
Рискните зайти обратно
to protect from covid-2019 drink beer corona

Process finished with exit code 0

```

Красиво ли мы написали наш код? Он выглядит не очень и через несколько лет наша фабрика будет ужасно выглядеть. Что полезного можно настраивать что будет полезно всем? Инжектить пропертисы из проперти файла. Это полезно всем. Напишем поддержку `@InjectProperty` отдельно.

Завтра к нам придут и скажут: "слушай, чувак, а давай сделаем ещё аннотацию для того чтобы могли инжектить объекты `@InjectObject` ", потом нам скажут: "о, давайте сделаем аннотацию `@Async` для всех методов которые должны работать асинхронно" и со временем у нас появляются `@Schedule`, `@Transactional` и т.д. Таким образом мы будем постоянно добавлять эти if'ы и проверять-проверять-проверять. Соответственно наша фабрика через пару лет будет на очень много тысяч строк кода.

Если вернуться к принципам SOLID, то второй принцип солида это Open Close Principle. Этот принцип говорит о том, что надо писать код таким образом (при чем даже инфраструктурный), что если захотим дописать какой-то новый функционал - это не приведет к тому что придется менять уже написанный закрытый код. Т.е. система должна быть открыта к выполнениям, но она должна быть закрыта к изменениям того что уже написано. 

Обычно принципы Single Responsibility и Open Close Principle ломаются вместе. Мы сломали Singlie Responsibility тем что наша фабрика должна отвечать за создание объектов, но хранит код который отвечает за настройку объекта. Поэтому делаем новый тип которым будет пользоваться фабрика, который будет называться `ObjectConfigurator` (то что в спринге называется `BeanPostProcessor`). Он будет интерфейсом поскольку `ObjectConfigurator` будет много. На каждую конвенцию/на каждую настройку будет свой конфигуратор. У конфигуратора будет метод `configure(Object t)` 

```java
package com.example;  
  
public interface ObjectConfigurator {  
    void configure(Object t);  
}
```

```java
package com.example;  
  
import lombok.SneakyThrows;  
  
import java.io.BufferedReader;  
import java.io.InputStream;  
import java.io.InputStreamReader;  
import java.lang.reflect.Field;  
import java.util.Map;  
import java.util.stream.Stream;  
  
import static java.util.stream.Collectors.toMap;  
  
public class InjectPropertyAnnotationObjectConfigurator implements ObjectConfigurator {  
    @Override  
    @SneakyThrows    public void configure(Object t) {  
        Class<?> implClass = t.getClass();  
        for (Field field : implClass.getDeclaredFields()) {  
            InjectProperty annotation = field.getAnnotation(InjectProperty.class);  
  
            InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("application.properties");  
//            String path = ClassLoader.getSystemClassLoader().getResource("application.properties").getPath();  
  
            Stream<String> lines = new BufferedReader(new InputStreamReader(inputStream)).lines();  
//            Stream<String> lines = new BufferedReader(new FileReader(path)).lines();  
            Map<String, String> propertiesMap = lines.map(line -> line.split("=")).collect(toMap(arr -> arr[0], arr -> arr[1]));  
  
            if (annotation != null) {  
                String value;  
                if (annotation.value().isEmpty()) {  
                    value = propertiesMap.get(field.getName());  
                } else {  
                    value = propertiesMap.get(annotation.value());  
                }  
  
                field.setAccessible(true);  
//                настраиваем объект t  
                field.set(t, value);  
            }  
        }  
    }  
}
```

Из этого кода вот эта `Map` не должна строиться каждый раз:

```java
            InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("application.properties");  
//            String path = ClassLoader.getSystemClassLoader().getResource("application.properties").getPath();  
  
            Stream<String> lines = new BufferedReader(new InputStreamReader(inputStream)).lines();  
//            Stream<String> lines = new BufferedReader(new FileReader(path)).lines();  
            Map<String, String> propertiesMap = lines.map(line -> line.split("=")).collect(toMap(arr -> arr[0], arr -> arr[1]));
```

Мы её построим один раз в конструкторе.

```java
package com.example;  
  
import lombok.SneakyThrows;  
  
import java.io.BufferedReader;  
import java.io.InputStream;  
import java.io.InputStreamReader;  
import java.lang.reflect.Field;  
import java.util.Map;  
import java.util.stream.Stream;  
  
import static java.util.stream.Collectors.toMap;  
  
public class InjectPropertyAnnotationObjectConfigurator implements ObjectConfigurator {  
  
    private Map<String, String> propertiesMap;  
  
    @SneakyThrows  
    public InjectPropertyAnnotationObjectConfigurator() {  
        InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("application.properties");  
        Stream<String> lines = new BufferedReader(new InputStreamReader(inputStream)).lines();  
        propertiesMap = lines.map(line -> line.split("=")).collect(toMap(arr -> arr[0], arr -> arr[1]));  
    }  
  
    @Override  
    @SneakyThrows    public void configure(Object t) {  
        Class<?> implClass = t.getClass();  
        for (Field field : implClass.getDeclaredFields()) {  
            InjectProperty annotation = field.getAnnotation(InjectProperty.class);  
  
            if (annotation != null) {  
                String value;  
                if (annotation.value().isEmpty()) {  
                    value = propertiesMap.get(field.getName());  
                } else {  
                    value = propertiesMap.get(annotation.value());  
                }  
  
                field.setAccessible(true);  
//                настраиваем объект t  
                field.set(t, value);  
            }  
        }  
    }  
}
```

Значит когда создатся конфигуратор `InjectPropertyAnnotationObjectConfigurator` - он себе один раз просетит мэпу из `application.properties` и после этого каждый раз  когда ему будут говорить "сконфигурируй объект" через `configure()` - он будет смотреть а нет ли у филдов данной аннотации `@InjectProperty` и если есть, то тогда будет доставать из `propertiesMap` нужное значение для данного проперти и сетить его в филд.

Теперь нам осталось подружить `ObjectFactory` с конфигураторами. В конструкторе проинициализируем следующим образом: было бы неплохо сканировать через тот же пакет который пришел из `JavaConfig` и поискать какие там имплементации конфигураторов существуют для того чтобы все их учесть и сложить в `List<ObjectConfigurator>`. Поэтому было бы не плохо если бы наш `Config` умел отдавать `Reflections getScanner()`.

```java
package com.example;  
  
import org.reflections.Reflections;  
  
import java.util.Map;  
import java.util.Set;  
  
public class JavaConfig implements Config {  
    private Reflections scanner;  
    private Map<Class, Class> ifc2ImplClass;  
  
    public JavaConfig(String packageToScan, Map<Class, Class> ifc2ImplClass) {  
        this.scanner = new Reflections(packageToScan);  
        this.ifc2ImplClass = ifc2ImplClass;  
    }  
  
    @Override  
    public <T> Class<? extends T> getImplClass(Class<T> ifc) {  
  
//        если наш JavaConfig не знает про имплементацию для интерфейса ничего  
        return ifc2ImplClass.computeIfAbsent(ifc, aClass -> {  
  
//        просим у сканнера все подвиды данного интерфейса.  
//        другими словами получаем все классы которые реализуют данный интерфейс  
            Set<Class<? extends T>> classes = scanner.getSubTypesOf(ifc);  
//        если у нас размер не является 1 (либо есть импл или их больше 1, то продолжать не можем)  
            if (classes.size() != 1) {  
                throw new RuntimeException(ifc + " has 0 or more than one implementation");  
            }  
  
//        Берём ту единственную имплементацию которую нашли  
            return classes.iterator().next();  
        });  
    }  
  
    @Override  
    public Reflections getScanner() {  
        return this.scanner;  
    }  
}
```

Т.е. наш конфиг умеет не только говорить какая имплементация, но и если какая-то инфраструктурная штука захочет получить сканер потому что сам хочет поискать что-то своё, то наш конфиг умеет этот сканнер отдать. А этот сканнер уже настроен потому что в наш `ObjectFactory` уже пришел готовый конфиг и когда этот конфиг создаётся - он настраивает этот сканнер.

```java
package com.example;  
  
import lombok.SneakyThrows;  
  
import java.util.ArrayList;  
import java.util.HashMap;  
import java.util.List;  
import java.util.Map;  
  
public class ObjectFactory {  
    private static ObjectFactory ourInstance = new ObjectFactory();  
    private List<ObjectConfigurator> configurators = new ArrayList<>();  
    private Config config;  
  
    public static ObjectFactory getInstance() {  
        return ourInstance;  
    }  
  
    @SneakyThrows  
    private ObjectFactory() {  
        this.config = new JavaConfig("com.example", new HashMap<>(Map.of(Policeman.class, AngryPoliceman.class)));  
//        берём все подвиды нашего ObjectConfigurator'ов  
        for (Class<? extends ObjectConfigurator> aClass : this.config.getScanner().getSubTypesOf(ObjectConfigurator.class)) {  
            configurators.add(aClass.getDeclaredConstructor().newInstance());  
        }  
    }  
  
    @SneakyThrows  
    //    Аналог getBean в спринге  
    public <T> T createObject(Class<T> type) {  
//        сначала будет равен type потому что а вдруг это гласс конкретный?  
        Class<? extends T> implClass = type;  
//        но делаем проверку если тип является интерфейсом, то тогда этот тип нужно заменить  
        if (implClass.isInterface()) {  
            implClass = config.getImplClass(type);  
        }  
  
        T t = implClass.getDeclaredConstructor().newInstance();  
  
//        берём все наши конфигураторы и просим каждого из них настроить наш объект  
        configurators.forEach(objectConfigurator -> objectConfigurator.configure(t));  
  
        return t;  
    }  
}
```

Этот дизайн-паттерн - некая разновидность Chain Of Responsibility когда вместо того чтобы нарушать Open Close Principle постоянно добавлять логику которая хендлит какой-то объект - вместо этого у нас есть много разных хендлеров, в нашем случае это много разных конфигураторов. Они автоматически добавляются в наш `List` и каждому мы даём возможность настроить объект.

Запустим и проверим что всё работает:

```txt
Начинаем дезинфекцию, всё вон!
to protect from covid-2019 drink beer corona
Всех убью! Вон пошли
зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад
Рискните зайти обратно
to protect from covid-2019 drink beer corona

Process finished with exit code 0
```

Ещё раз проверим кое-что. Изменим `application.properties`:

```txt
alcohol=beer corona  
wisky=singleton
```

```java
package com.example;  
  
public class RecommendatorImpl implements Recommendator {  
    @InjectProperty("wisky")  
    private String alcohol;  
  
    @Override  
    public void recommend() {  
        System.out.println("to protect from covid-2019 drink " + alcohol);  
    }  
}
```

Запустим и видим что все работает корректно:

```txt
Начинаем дезинфекцию, всё вон!
to protect from covid-2019 drink singleton
Всех убью! Вон пошли
зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад
Рискните зайти обратно
to protect from covid-2019 drink singleton

Process finished with exit code 0
```

**Подведём промежуточные итоги**

У нас есть следующее:
1. Фабрика, которая является централизованным местом для создания объектов. Если нам нужна новая имплементация, то не нужно лезть в код. 
2. Если хотим чтобы объект приходил настроенным, то можем обучать нашу фабрику настраивать объект согласно конвенциями типа `@PropertySource`, `@InjectProperty` и т.д.
3. Сможем кешировать синглтоны. Т.е. большинство объектов которые будут настраиваться нашей фабрикой будут не объекты типа `Person`, а объекты типа `PersonService` (всякие разные сервисы, репозитории, контроллеры и т.д.) - они в 99% случаев нужны нам в единственном экземпляре, то соответственно мы сможем их кешировать.

**Поговорим почему Singletone это одновременно и хорошо, и анти-паттерн**.

Есть два вида объектов - Data Objects (типа наша модель) и эти объекты никогда не управляются таким фреймворком как фабрика. Они управляются всякими Hibernate'ами, Spring Data'ми и т.д. потому что эти объекты постоянно создаются и все время наполняются разными данными которые приходят либо с базы, либо приходят по REST и т.д.

А есть объекта типа Services. Эти объекты типа Services лучше всего делать как stateless. Что нам даёт возможность сделать их Singletone'ами. **Stateless это когда наш сервис при создании настроился и его состояние потом уже меняться не будет.** Это хорошо потому что мы не должны никогда заморачиваться над тем, а что произойдет если мой синглтон

Если мой объект функциональный, в нем есть прикольные функциональные методы которыми можно пользоваться в разных точках - зачем нам его клонировать и каждый раз создавать по новому. Мы хотим пользоваться тем же самым объектом. Есть ситуации когда создание клона будет просто бить по перфомансу, но во многих случаев это может просто нарушить бизнес-логику. 

Т.е. практически все наши сервисы должны быть синглтонами и в случае если они Stateless, то с этим нет никакой проблемы потому что если синглтон попал в разные классы (т.е. разные объекты пользуются одним  тем же синглтоном), то никто не должен думать "а что собственно произойдёт, я вот сейчас вызываю у синглтона какой-то бизнес-метод, а вдруг сейчас кто-то другой кто тоже пользуется синглтоном - его там переконфигурировал в процессе. и соответственно в середине вызова этого метода там бах и что-то поменялось". 

Так вот если наш синглтон Stateless, то такого произойти не может. Мы не должны думать про синхронизацию, про локи или не придется клонировать. Поэтому stateless это хорошо, мы стараемся делать stateless и тогда возникает вопрос почему бы не делать синглтон. 

Здесь хочется вернуться в класс `CoronaDesinfector`

```java
package com.example;  
  
public class CoronaDesinfector {  
  
    private Announcer announcer = ObjectFactory.getInstance().createObject(Announcer.class);  
    private Policeman policeman = ObjectFactory.getInstance().createObject(Policeman.class);  
  
    public void start(Room room) {  
        announcer.announce("Начинаем дезинфекцию, всё вон!");  
        policeman.makePeopleLeaveRoom();  
        desinfect(room);  
        announcer.announce("Рискните зайти обратно");  
    }  
  
    private void desinfect(Room room) {  
        System.out.println("зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад");  
    }  
}
```

Полным говнищем наш класс делает вот эта часть:

```java
= ObjectFactory.getInstance().createObject(Announcer.class);  
= ObjectFactory.getInstance().createObject(Policeman.class);
```

Есть много объяснений почему это плохо. 

Это не инверсия контроля, это lookup! Мы сейчас начинаем говорить о том что вместо того чтобы сделать инверсию контроля до чего эволюция ООП доросла чуть позже - мы сделали lookup на котором все сидели до того как появилась инверсия контроля. 

Т.е. все поняли что Object Factory это классно, поскольку Object Factory нужна всем, то это естественно домашний синглтон (это синглтоны которые можно получить при помощи `getInstance()`) и соответственно все кто хочет от фабрики (а от фабрики хотят все что-то) - они везде его через статический метод дергают.

Давайте посмотрим что произойдет если придет новый начальник и скажет: "ломбок с 14 джавой плохо дружит поэтому давайте `@SneakyThrows` выкинем и этот ексепшен будем декларировать"

```java
package com.example;  
  
import lombok.SneakyThrows;  
  
import java.util.ArrayList;  
import java.util.HashMap;  
import java.util.List;  
import java.util.Map;  
  
public class ObjectFactory {  
    private static ObjectFactory ourInstance = new ObjectFactory();  
    private List<ObjectConfigurator> configurators = new ArrayList<>();  
    private Config config;  
  
    public static ObjectFactory getInstance() {  
        return ourInstance;  
    }  
  
    @SneakyThrows  
    private ObjectFactory() {  
        this.config = new JavaConfig("com.example", new HashMap<>(Map.of(Policeman.class, AngryPoliceman.class)));  
//        берём все подвиды нашего ObjectConfigurator'ов  
        for (Class<? extends ObjectConfigurator> aClass : this.config.getScanner().getSubTypesOf(ObjectConfigurator.class)) {  
            configurators.add(aClass.getDeclaredConstructor().newInstance());  
        }  
    }  
      
    //    Аналог getBean в спринге  
    public <T> T createObject(Class<T> type) throws Exception {  
//        сначала будет равен type потому что а вдруг это гласс конкретный?  
        Class<? extends T> implClass = type;  
//        но делаем проверку если тип является интерфейсом, то тогда этот тип нужно заменить  
        if (implClass.isInterface()) {  
            implClass = config.getImplClass(type);  
        }  
  
        T t = implClass.getDeclaredConstructor().newInstance();  
  
//        берём все наши конфигураторы и просим каждого из них настроить наш объект  
        configurators.forEach(objectConfigurator -> objectConfigurator.configure(t));  
  
        return t;  
    }  
}
```

и возвращаемся во все классы которые используют `ObjectFactory` и видим что теперь не компилируется. Это была проблема №1. Проблема №2. Допустим это даже не произошло (вернём обратно)

```java
    @SneakyThrows  
    //    Аналог getBean в спринге  
    public <T> T createObject(Class<T> type) {  
//        сначала будет равен type потому что а вдруг это гласс конкретный?  
        Class<? extends T> implClass = type;  
//        но делаем проверку если тип является интерфейсом, то тогда этот тип нужно заменить  
        if (implClass.isInterface()) {  
            implClass = config.getImplClass(type);  
        }  
  
        T t = implClass.getDeclaredConstructor().newInstance();  
  
//        берём все наши конфигураторы и просим каждого из них настроить наш объект  
        configurators.forEach(objectConfigurator -> objectConfigurator.configure(t));  
  
        return t;  
    }
```

но мы сейчас хотим написать юнит-тест на `CoronaDesinfector`. Как мы его собственно напишем? Мы хотим протестировать что метод `start()` работает хорошо. Мы естественно засетим в `Announcer` какой-то мок, в `Policeman` тоже какой-то мок. Но в тот момент когда в юнит тесте кто-то создаст при помощи new этот `CoronaDesinfector` - в этот же момент у меня в любом случае дёрнется `ObjectFactory.getInstance()` потому что до того как отрабатывают конструкторы - отрабатывают inline'ы. И соответственно эти филды все равно изначально проинициализируются при помощи фабрик. А фабрик создаст конфигурацию которая сейчас захардкожена:

```java
    @SneakyThrows  
    private ObjectFactory() {  
        this.config = new JavaConfig("com.example", new HashMap<>(Map.of(Policeman.class, AngryPoliceman.class)));  
//        берём все подвиды нашего ObjectConfigurator'ов  
        for (Class<? extends ObjectConfigurator> aClass : this.config.getScanner().getSubTypesOf(ObjectConfigurator.class)) {  
            configurators.add(aClass.getDeclaredConstructor().newInstance());  
        }  
    }
```

берёт какой-то сканнер, начинает сканировать какие-то пакеты, искать какие-то классы, складывать и т.д. 

Если оставим `ObjectFactory.getInstance().createObject(Announcer.class)` то получается что каждый тест любого класса который работает с `ObjectFactory` будет бежать как минимум 2 секунды или нам придется при помощи power-mock начинать мокать объект `getInstance()`, а люди которые работали с power-mock знают что он убивает перфоманс ещё лучше потому что он classloader'ы подменяет и вообще кошмар и ужас.

Последний аргумент. Мы начали говорить что было бы здорово если бы у нас наша инфраструктура могла кешировать синглтоны поскольку синглтоны это классная вещи, сервисы должны быть синглтонами в большинстве своем. Но если мы будем реализовывать паттерн Singletone так как это было в 2003 году.

**Почему домашний синглтон это плохо?** Забудем про фабрику. У нас есть сервис А, который нужен сервису Б, который нужен сервису Ц, Д и т.д. Все они синглтоны и все они друг другом пользуются при помощи `getInstance()`. Мы не сможем написать юнит тест на какой нибудь из этих сервисов никогда в жизни потому что создание сервиса А приведет к тому что он потянет за собой сервис Б, который в свою очередь потянет Ц, Д и у нас идет такая длинная цепочка при помощи статических методов потому что объекты между собой связаны статическими объектами и хрен что подложишь/подвинешь. Поэтому это и есть антипаттерн.

Т.е. синглтон это классно, но реализовывать его это плохо. Таким образом мы говорим: чем копипастить это дурацкое решение с методом `getInstance()` по всем нашим классам и отказаться от фабрики давайте сделаем наоборот - научим нашу инфраструктуру кешировать все синглтоны.

*Но давайте сначала проверим что у нас с синглтонами есть проблема.* 

Допустим, у нас есть `Announcer` в котором получаем `Reccomendator`. Завтра мы захотим этот рекомендатор заинжектить в ещё какое-то место. Опять же если у нас будет написан `ObjectFactory.getInstance().createObject(Recommendator.class)`, то он ещё раз дёрнет `createObject()` и у нас ещё раз создастся дополнительный инстанс. Мы хотим от этого уйти, мы хотим эти синглтоны кешировать.

Если мы хотим кешировать синглтоны, то во первых наверное у нас появится какая-то магическая аннотация которой будем помечать те классы которые хотят рассказать о себе что они синглтоны `@Singleton` и соответственно наша инфраструктура будет кешировать все объекты которые сказали "мы синглтон".

```java
package com.example;  
  
public @interface Singleton {  
}
```

Ну или можно сделать по умолчанию наоборот как в спринге. В спринге по умолчанию все объекты - синглтоны и надо явно при помощи аннотации `@Scope` указать что это PROTOTYPE или какой-то другой скоуп.

Мы хотим поддерживать нашу `ObjectFactory` в красивом состоянии. Если мы сейчас навесим на наш `ObjectFactory` дополнительные респонсобилити кешировать объекты, то это будет не очень правильно. Т.е. правильно будет что у нас будет какой-то другой уровень абстракции. У нас будет какой-то класс который будет называться `ApplicationContext` (также как и в спринге). У него будет доступ к нашему `ObjectFactory` и если контекст захочет создать объект, то он в эту фабрику сходит, но у него кроме этого будет ещё `Map<Class, Object>` в котором будут уже закешированы все объекты которые были созданы и которые являются синглтонами.

```java
package com.example;  
  
import java.util.Map;  
import java.util.concurrent.ConcurrentHashMap;  
  
public class ApplicationContext {  
    private ObjectFactory factory;  
    private Map<Class, Object> cache = new ConcurrentHashMap<>();  
}
```

Соответственно у нас в этом контексте будет замечательный метод который будет возвращать тип и называться будет не createObject(), а будет `getObject()` потому что неизвестно создастся ли новый или вернется уже существующий.

```java
package com.example;  
  
import java.util.Map;  
import java.util.concurrent.ConcurrentHashMap;  
  
public class ApplicationContext {  
    private ObjectFactory factory;  
    private Map<Class, Object> cache = new ConcurrentHashMap<>();  
  
    //    Class<T> type - желаемый тип объекта  
    public <T> T getObject(Class<T> type) {  
        return null;  
    }  
}
```

Пока оставим null. Что произойдет когда закончим разработку?

Мы сейчас скажем всем нашим программистам: "друзья нашим, вы помните что мы работаем с инфраструктурой центром которой/пупом земли которой являлся `ObjectFactory` и у вас наверное в некоторых местах написан такой код: `ObjectFactory.getInstance().createObject(...);`. Вот мы сейчас попросим заменить этот код на `application.getBean()`. Не надо делать `createObject()`, надо делать `getObject()`".

Нам конечно же скажут разработчики: "чувак ты вообще охренел что ли?! мы уже 4 года на `ObjectFactory` сидели, всё было замечательно, уже 800 000 строк кода написано с `сreateObject()` ты хочешь чтобы мы поменяли? А все наши клиенты тоже будут менять? А в опенсоурс выложили тоже будут менять?"

Т.е. это очень плохо то что у нас было. Это называется **утечка инфраструктуры внутрь бизнес-логики**. Вот эта концепция *инверсия контроля* - она как раз говорит о том *что для того чтобы инфраструктурный код что-то сделал полезное для нас - неправильно чтобы наши сервисы бегали в инфраструктурный код и что-то от него просили*, потому что если мой сервис знает как разговаривать с фабрикой, то это значит что он на ней завязан, знает её API, не даст возможности поменять этот API завтра. Это не даст возможности заменить одну фабрику на какую-нибудь другую фабрику или перейти на спринг. Это не даст возможности написать тесты. Это делает огромное кол-во проблем поэтому нам нужна инверсия контролей.

Что говорит инверсия контроля: "не вы будете дергать наши методы для того чтобы мы настроили ваши объекты, а мы сами будем создавая ваши объекты понимать как их нужно настроить и будем их настраивать". Точно также как мы сделали с аннотацией `@InjectProperty`.

Вместо того чтобы настраивать филды самому - `CoronaDesinfector` попросит настроить свои филды у фабрики через аннотацию `@InjectByType`

```java
package com.example;  
  
public class CoronaDesinfector {  
  
    @InjectByType  
    private Announcer announcer;  
    @InjectByType  
    private Policeman policeman;  
  
    public void start(Room room) {  
        announcer.announce("Начинаем дезинфекцию, всё вон!");  
        policeman.makePeopleLeaveRoom();  
        desinfect(room);  
        announcer.announce("Рискните зайти обратно");  
    }  
  
    private void desinfect(Room room) {  
        System.out.println("зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад");  
    }  
}
```

```java
package com.example;  
  
import java.lang.annotation.Retention;  
import java.lang.annotation.RetentionPolicy;  
  
@Retention(RetentionPolicy.RUNTIME)  
public @interface InjectByType {  
}
```

Соответственно теперь нам нужен аналогичный `ObjectConfigurator` который будет отвечать за эту аннотацию.

Помним, что аннотация это не магия, а маркер/способ сообщить нашей инфраструктуре чего мы здесь хотели. Поэтому будем сообщать

```java
package com.example;  
  
import lombok.SneakyThrows;  
  
import java.lang.reflect.Field;  
  
public class InjectByTypeAnnotationObjectConfigurator implements ObjectConfigurator {  
    @Override  
    @SneakyThrows    public void configure(Object t) {  
//        берём все филды  
        for (Field field : t.getClass().getDeclaredFields()) {  
//            если у нас филд аннотирован аннотацией @InjectByType,   
if (field.isAnnotationPresent(InjectByType.class)) {  
//                то тогда хотим просетить этот филд объектом который подходит по типу этого филда  
                field.setAccessible(true);  
                Object object = ObjectFactory.getInstance().createObject(field.getType());  
                field.set(t, object);  
            }  
        }  
    }  
}
```

В тот момент когда у нас появилась инверсия контролей - нельзя больше создавать объекты через new. Раньше создавали основной объект через new, а он настраивал себя сам ходя в `ObjectFactory`. Теперь у нас другая ситуация - у нас `ObjectFactory` будет создавать `CoronaDesinfector` и он его сам настроит. 

```java
package com.example;  
  
public class App   
{  
    public static void main( String[] args )  
    {  
        CoronaDesinfector coronaDesinfector = new CoronaDesinfector();  
        coronaDesinfector.start(new Room());  
    }  
}
```

```java
package com.example;  
  
public class App   
{  
    public static void main( String[] args )  
    {  
        CoronaDesinfector coronaDesinfector = ObjectFactory.getInstance().createObject(CoronaDesinfector.class);  
        coronaDesinfector.start(new Room());  
    }  
}
```

Запускаем и проверяем что работает:

```txt
Начинаем дезинфекцию, всё вон!
to protect from covid-2019 drink singleton
Всех убью! Вон пошли
зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад
Рискните зайти обратно
to protect from covid-2019 drink singleton

Process finished with exit code 0

```

Видим что у нас все замечательно заработало, но это же плохо что `ObjectFactory.getInstance().createObject(CoronaDesinfector.class);` протекает в бизнес-логику. Во первых `ObjectFactory` уйдёт везде за исключением двух мест:

1. `ObjectFactory` останется в `InjectByTypeAnnotationObjectConfigurator` и в этом нет ничего страшного поскольку это инфраструктурный класс. То что у нас есть утечка инфраструктуры внутрь инфраструктуры - это скрыто от бизнес логики и в этом нет никакой проблемы.
2. Вторая точка где будет `ObjectFactory` - это `main()` метод. `main()` метод это не бизнес-логика. Это можно сказать конфигурационный код который говорит о том какой сервис мы хотим создать и запустить.

**Доделаем логику в `ApplicationContext` чтобы наш фреймворк умел кешировать синглтон.**

Значит как мы будем действовать. Когда будет вызываться метод `getObject()`. Во первых что нам нужно для нашего `ApplicationContext`'а. Нам нужен будет наверняка `Config` и этот конфиг уже будет сюда передаваться через конструктор. Напоминание: *конфиг содержит информацию о том, какие пакеты надо сканировать и какие соответственно имплементации у нас есть в `Map`* и человек который будет создавать конфигурацию - он будет соответственно это передавать.

Первым что мы должны сделать - это убрать часть в `createObject()` из `ObjectFactory`:

```java
    @SneakyThrows  
    //    Аналог getBean в спринге  
    public <T> T createObject(Class<T> type) {  
//        сначала будет равен type потому что а вдруг это гласс конкретный?  
        Class<? extends T> implClass = type;  
//        но делаем проверку если тип является интерфейсом, то тогда этот тип нужно заменить  
        if (implClass.isInterface()) {  
            implClass = config.getImplClass(type);  
        }  
  
        T t = implClass.getDeclaredConstructor().newInstance();  
  
//        берём все наши конфигураторы и просим каждого из них настроить наш объект  
        configurators.forEach(objectConfigurator -> objectConfigurator.configure(t));  
  
        return t;  
    }
```

```java
    @SneakyThrows  
    //    Аналог getBean в спринге  
    public <T> T createObject(Class<T> type) {  
        T t = implClass.getDeclaredConstructor().newInstance();  
  
//        берём все наши конфигураторы и просим каждого из них настроить наш объект  
        configurators.forEach(objectConfigurator -> objectConfigurator.configure(t));  
  
        return t;  
    }
```

Это сделали потому что когда кто-то будет обращаться в контекст и говорить "мне нужен Announcer" - он уже будет давать интерфейс и соответственно об этом интерфейсе должен знать наш контекст потому что он будет кешировать против этого интерфейса имплементацию которую фабрика создаст.

```java
package com.example;  
  
import java.util.Map;  
import java.util.concurrent.ConcurrentHashMap;  
  
public class ApplicationContext {  
    private ObjectFactory factory;  
    private Map<Class, Object> cache = new ConcurrentHashMap<>();  
    private Config config;  
  
    //    Class<T> type - желаемый тип объекта  
    public <T> T getObject(Class<T> type) {  
//        сначала будет равен type потому что а вдруг это гласс конкретный?  
        Class<? extends T> implClass = type;  
  
//        если наш кеш уже содержит в себе этот тип, то просто вернем из нашего кеша то что там лежит (уже созданный готовый объект)  
        if (cache.containsKey(implClass)) {  
            return (T) cache.get(implClass);  
        }  
  
//        если у нас нет в кеше этого объекта, то сейчас надо его создавать и настраивать  
        if (implClass.isInterface()) {  
            implClass = config.getImplClass(type);  
        }  
  
//        получаем настроенный тип  
        T t = factory.createObject(implClass);  
  
//        проверяем не надо ли этот объект положить в наш кэш  
        if (implClass.isAnnotationPresent(Singleton.class)) {  
            cache.put(type, t);  
        }  
  
        return t;  
    }  
}
```

Когда к контексту обращаются и говорят "дай мне ещё раз объект такого-то типа (например в  `getObject(Class<T> type)` пришел интерфейс полицейского)" говорим: "может быть этот синглтон уже был создан? если да, забери его. Если ещё не был создан, то сначала хотим проверить тип интерфейс ли это. Если интерфейс, то просим у конфига имплементацию и как только получили имплементацию мы её создаём. И дальше либо мы его либо кешируем, либо не кешируем в зависимости от того синглтон ли он. А потом в конце в любом случае возвращаем"

Нам нужен ещё какой-то раннер для связывания контекста и фабрики. У нас получается так, что наш контекст обязан зависеть от фабрики (видим это в конструкторе) потому что он при помощи фабрики объекты создаёт когда они нужны. С другой стороны наша фабрика обязательно должна зависеть от контекста.

Почему фабрика должна зависеть от контекста? У нас есть замечательный `ObjectConfigurator` который отвечает за injection. Если раньше injection делался при помощи фабрики, то теперь это тоже становится неправильным потому что теперь если он инжектит объект который уже является синглтоном, то нафига ему ещё создавать. Т.е. нам нужно в `ObjectConfigurator`'ах не обращаться к `ObjectFactory.getInstance().createObject()` , а брать из контекста.

```java
package com.example;  
  
import lombok.SneakyThrows;  
  
import java.lang.reflect.Field;  
  
public class InjectByTypeAnnotationObjectConfigurator implements ObjectConfigurator {  
    @Override  
    @SneakyThrows    public void configure(Object t) {  
//        берём все филды  
        for (Field field : t.getClass().getDeclaredFields()) {  
//            если у нас филд аннотирован аннотацией @InjectByType,  
            if (field.isAnnotationPresent(InjectByType.class)) {  
//                то тогда хотим просетить этот филд объектом который подходит по типу этого филда  
                field.setAccessible(true);  
                Object object = ObjectFactory.getInstance().createObject(field.getType());  
                field.set(t, object);  
            }  
        }  
    }  
}
```

```java
package com.example;  
  
import lombok.SneakyThrows;  
  
import java.lang.reflect.Field;  
  
public class InjectByTypeAnnotationObjectConfigurator implements ObjectConfigurator {  
    @Override  
    @SneakyThrows    public void configure(Object t) {  
//        берём все филды  
        for (Field field : t.getClass().getDeclaredFields()) {  
//            если у нас филд аннотирован аннотацией @InjectByType,  
            if (field.isAnnotationPresent(InjectByType.class)) {  
//                то тогда хотим просетить этот филд объектом который подходит по типу этого филда  
                field.setAccessible(true);  
                Object object = context.getObject(field.getType());  
                field.set(t, object);  
            }  
        }  
    }  
}
```

но тогда вопрос - откуда у нас здесь появится контекст? Нам надо его откуда-то прокинуть. Метод `configure()` у нас вызывает наша фабрика когда она создала объект `T t`  и в этот `configure()` наша фабрика должна передать контекст. Почему потому что как минимум один из конфигураторов в этом контексте нуждается.

```java
package com.example;  
  
import lombok.SneakyThrows;  
  
import java.lang.reflect.Field;  
  
public class InjectByTypeAnnotationObjectConfigurator implements ObjectConfigurator {  
    @Override  
    @SneakyThrows    public void configure(Object t, ApplicationContext context) {  
//        берём все филды  
        for (Field field : t.getClass().getDeclaredFields()) {  
//            если у нас филд аннотирован аннотацией @InjectByType,  
            if (field.isAnnotationPresent(InjectByType.class)) {  
//                то тогда хотим просетить этот филд объектом который подходит по типу этого филда  
                field.setAccessible(true);  
                Object object = context.getObject(field.getType());  
                field.set(t, object);  
            }  
        }  
    }  
}
```

```java
package com.example;  
  
public interface ObjectConfigurator {  
    void configure(Object t, ApplicationContext context);  
}
```

Так вот что у нас получается. Мы говорим о том, что эти `ObjectConfigurator`'ы которые бог знает сколько их будет в будущем - очень может быть что некоторые из них захотят пользоваться контекстом. Например конфигуратор который отвечает за инжектион по пропертисам - ему сегодня контекст не нужен и им не пользуется. Но может быть завтра в контексте будет сидеть объект `Environment` в котором будут находится какие-то пропертисы которые собрались в самом начале из env variables и соответственно этот конфигуратор тоже захочет слазить в контекст чтобы их вытащить и добавить в `Map<String, String> propertiesMap`

```java
package com.example;  
  
import lombok.SneakyThrows;  
  
import java.io.BufferedReader;  
import java.io.InputStream;  
import java.io.InputStreamReader;  
import java.lang.reflect.Field;  
import java.util.Map;  
import java.util.stream.Stream;  
  
import static java.util.stream.Collectors.toMap;  
  
public class InjectPropertyAnnotationObjectConfigurator implements ObjectConfigurator {  
  
    private Map<String, String> propertiesMap;  
  
    @SneakyThrows  
    public InjectPropertyAnnotationObjectConfigurator() {  
        InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("application.properties");  
        Stream<String> lines = new BufferedReader(new InputStreamReader(inputStream)).lines();  
        propertiesMap = lines.map(line -> line.split("=")).collect(toMap(arr -> arr[0], arr -> arr[1]));  
    }  
  
    @Override  
    @SneakyThrows    public void configure(Object t, ApplicationContext context) {  
        Class<?> implClass = t.getClass();  
        for (Field field : implClass.getDeclaredFields()) {  
            InjectProperty annotation = field.getAnnotation(InjectProperty.class);  
  
            if (annotation != null) {  
                String value;  
                if (annotation.value().isEmpty()) {  
                    value = propertiesMap.get(field.getName());  
                } else {  
                    value = propertiesMap.get(annotation.value());  
                }  
  
                field.setAccessible(true);  
//                настраиваем объект t  
                field.set(t, value);  
            }  
        }  
    }  
}
```

Это в принципе неплохо чтобы наш конфигуратор получал здесь контекст.

**Чиним `ObjectFactory`**

После всего что мы написали получается что нашей фабрике нужен весь контекст, а `Config` мы можем убрать. Если наша фабрика получает `ApplicationContext`, то `Config` может вытащить из него. Нашей фабрике нужен весь контекст, а конфиги может и так достать из контекста.

Все конфигураторы которые хотят сконфигуриировать объект имеют доступ к контексту.

```java
package com.example;  
  
import lombok.SneakyThrows;  
  
import java.util.ArrayList;  
import java.util.List;  
  
public class ObjectFactory {  
    private static ObjectFactory ourInstance = new ObjectFactory();  
    private final ApplicationContext context;  
    private List<ObjectConfigurator> configurators = new ArrayList<>();  
  
  
    public static ObjectFactory getInstance() {  
        return ourInstance;  
    }  
  
    @SneakyThrows  
    public ObjectFactory(ApplicationContext context) {  
        this.context = context;  
//        берём все подвиды нашего ObjectConfigurator'ов из контекста  
        for (Class<? extends ObjectConfigurator> aClass : context.getConfig().getScanner().getSubTypesOf(ObjectConfigurator.class)) {  
            configurators.add(aClass.getDeclaredConstructor().newInstance());  
        }  
    }  
  
    @SneakyThrows  
    public <T> T createObject(Class<T> implClass) {  
        T t = implClass.getDeclaredConstructor().newInstance();  
  
//        берём все наши конфигураторы и просим каждого из них настроить наш объект  
        configurators.forEach(objectConfigurator -> objectConfigurator.configure(t, context));  
  
        return t;  
    }  
}
```

**Теперь почему нам придется делать нам какой-то Runner**

Если мы в классе `ApplicationContext` сделаем следующее:

Нам фабрику `private ObjectFactory factory` надо создать, но когда мы её создаём в конструкторе, то нам придется в фабрику передать контекст:

```java
public ApplicationContext(Config config) {  
    this.config = config;  
    ObjectFactory objectFactory = new ObjectFactory(this);  
}
```

Такой код очень плохой. У нас ещё не отработал конструктор `ApplicationContext`, а уже начинает работать конструктор `ObjectFactory` который хочет этим контекстом пользоваться и ничем хорошим это не закончится если мы захотим при поднятии контекста проинициализировать все синглтоны которые не ленивые. 

Поэтому сделаем класс, который очень похож на класс spring boot который называется `SpringApplication` с методом `run()`, а у нас будет класс который будет называться `Application` и у этого класса будет статически метод как и в спрингбуте `run()`. У нас будет метод, который возвращает `ApplicationContext`, который называется `run()` , который принимает путь сканирования пакетов и принимает мэпу  интерфейсов и реализаций `run(String packageToScan, Map<Class, Class> ifc2ImplClass)`.

Он из этого строит `JavaConfig` куда передаем пакет и мэпу.

После этого строим контекст передавая конфиг.

После этого мы можем построить `ObjectFactory` который обязательно хочет контекст получить. Это лучше чем передавать this со стороны.

После этого если бы я захотел - проинициализировать все синглтоны которые не ленивые (просканировать пакеты, поискать аннотацию синглтон, понасоздавать объект и засунуть в контекст)

И дальше в контекст засетить `objectFactory`

После этого можем сказать что контекст создан.

```java
package com.example;  
  
import java.util.Map;  
  
public class ApplicationRunner {  
    public static ApplicationContext run(String packageToScan, Map<Class, Class> if2cImplClass) {  
        JavaConfig config = new JavaConfig(packageToScan, if2cImplClass);  
        ApplicationContext context = new ApplicationContext(config);  
        ObjectFactory objectFactory = new ObjectFactory(context);  
        context.setFactory(objectFactory);  
        return context;  
    }  
}
```

```java
package com.example;  
  
import lombok.SneakyThrows;  
  
import java.util.ArrayList;  
import java.util.List;  
  
public class ObjectFactory {  
    private final ApplicationContext context;  
    private List<ObjectConfigurator> configurators = new ArrayList<>();  
  
  
    @SneakyThrows  
    public ObjectFactory(ApplicationContext context) {  
        this.context = context;  
//        берём все подвиды нашего ObjectConfigurator'ов из контекста  
        for (Class<? extends ObjectConfigurator> aClass : context.getConfig().getScanner().getSubTypesOf(ObjectConfigurator.class)) {  
            configurators.add(aClass.getDeclaredConstructor().newInstance());  
        }  
    }  
  
    @SneakyThrows  
    public <T> T createObject(Class<T> implClass) {  
        T t = implClass.getDeclaredConstructor().newInstance();  
  
//        берём все наши конфигураторы и просим каждого из них настроить наш объект  
        configurators.forEach(objectConfigurator -> objectConfigurator.configure(t, context));  
  
        return t;  
    }  
}
```

```java
package com.example;  
  
import lombok.Getter;  
import lombok.Setter;  
  
import java.util.Map;  
import java.util.concurrent.ConcurrentHashMap;  
  
public class ApplicationContext {  
    @Setter  
    private ObjectFactory factory;  
    private Map<Class, Object> cache = new ConcurrentHashMap<>();  
    @Getter  
    private Config config;  
  
    public ApplicationContext(Config config) {  
        this.config = config;  
    }  
  
    //    Class<T> type - желаемый тип объекта  
    public <T> T getObject(Class<T> type) {  
//        сначала будет равен type потому что а вдруг это гласс конкретный?  
        Class<? extends T> implClass = type;  
  
//        если наш кеш уже содержит в себе этот тип, то просто вернем из нашего кеша то что там лежит (уже созданный готовый объект)  
        if (cache.containsKey(implClass)) {  
            return (T) cache.get(implClass);  
        }  
  
//        если у нас нет в кеше этого объекта, то сейчас надо его создавать и настраивать  
        if (implClass.isInterface()) {  
            implClass = config.getImplClass(type);  
        }  
  
//        получаем настроенный тип  
        T t = factory.createObject(implClass);  
  
//        проверяем не надо ли этот объект положить в наш кэш  
        if (implClass.isAnnotationPresent(Singleton.class)) {  
            cache.put(type, t);  
        }  
  
        return t;  
    }  
}
```

Теперь вот как будет выглядеть наш `main()`.

В нашем мейне мы теперь не будем создавать `CoronaDesinfector` таким странным путём:

```java
package com.example;  
  
public class App   
{  
    public static void main( String[] args )  
    {  
        CoronaDesinfector coronaDesinfector = ObjectFactory.getInstance().createObject(CoronaDesinfector.class);  
        coronaDesinfector.start(new Room());  
    }  
}
```

Мы вместо этого будем вызывать `ApplicationRunner.run()` и передавать в него пакет и мапу интерфейсов-реализаций для последующей настройки.

```java
package com.example;  
  
import java.util.HashMap;  
import java.util.Map;  
  
public class App {  
    public static void main(String[] args) {  
        ApplicationContext context = ApplicationRunner.run(  
                "com.example",  
                new HashMap<>(Map.of(Policeman.class, PolicemanImpl.class))  
        );  
        CoronaDesinfector desinfector = context.getObject(CoronaDesinfector.class);  
        desinfector.start(new Room());  
    }  
}
```

Если мы попробуем запустить, то у нас зафейлится запуск поскольку нам надлоо чинить код в других местах где используется старый вариант взаимодействия с архитектурой:

```java
package com.example;  
  
public class ConsoleAnnouncer implements Announcer {  
  
    private Recommendator recommendator = ObjectFactory.getInstance().createObject(Recommendator.class);  
  
    @Override  
    public void announce(String message) {  
        System.out.println(message);  
        recommendator.recommend();  
    }  
}
```

```java
package com.example;  
  
public class ConsoleAnnouncer implements Announcer {  
  
    @InjectByType  
    private Recommendator recommendator;  
  
    @Override  
    public void announce(String message) {  
        System.out.println(message);  
        recommendator.recommend();  
    }  
}
```

Теперь попробуем запустить и видим что все работает как надо:

```txt
Начинаем дезинфекцию, всё вон!
to protect from covid-2019 drink singleton
пиф паф, бах бах, кыш, кыш!
зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад
Рискните зайти обратно
to protect from covid-2019 drink singleton

Process finished with exit code 0
```

Теперь сделаем следующее: мы хотим в корона-рекоммендаторе (`RecommendatorImpl`) сделать конструктор в котором будем печатать что он был создан чтобы видеть сколько раз он создается.

```java
package com.example;  
  
@Singleton  
public class RecommendatorImpl implements Recommendator {  
    @InjectProperty("wisky")  
    private String alcohol;  
  
    public RecommendatorImpl() {  
        System.out.println("RecommendatorImpl was created");  
    }  
  
    @Override  
    public void recommend() {  
        System.out.println("to protect from covid-2019 drink " + alcohol);  
    }  
}
```

Запустим и увидим что он используется у нас один раз:

```txt
RecommendatorImpl was created
Начинаем дезинфекцию, всё вон!
to protect from covid-2019 drink singleton
пиф паф, бах бах, кыш, кыш!
зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад
Рискните зайти обратно
to protect from covid-2019 drink singleton

Process finished with exit code 0
```

Но если мы его сейчас заинжектим в какой-то другой сервис + уберём аннотацию `Singleton`, например `PolicemanImpl` и даже не будем им пользоваться:

```java
package com.example;  
  
//@Singleton  
public class RecommendatorImpl implements Recommendator {  
    @InjectProperty("wisky")  
    private String alcohol;  
  
    public RecommendatorImpl() {  
        System.out.println("RecommendatorImpl was created");  
    }  
  
    @Override  
    public void recommend() {  
        System.out.println("to protect from covid-2019 drink " + alcohol);  
    }  
}
```

```java
package com.example;  
  
public class PolicemanImpl implements Policeman {  
  
    @InjectByType  
    private Recommendator recommendator;  
  
    @Override  
    public void makePeopleLeaveRoom() {  
        System.out.println("пиф паф, бах бах, кыш, кыш!");  
    }  
}
```

Запустим и видим что как только `Policeman` будет создан, то наш `Recommendator` будет создаваться два раза потому то каждому нужен свой `Recommendator`:

```txt
RecommendatorImpl was created
RecommendatorImpl was created
Начинаем дезинфекцию, всё вон!
to protect from covid-2019 drink singleton
пиф паф, бах бах, кыш, кыш!
зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад
Рискните зайти обратно
to protect from covid-2019 drink singleton

Process finished with exit code 0
```

А если поставим одну аннотацию, то будет один раз создаваться для всех:

```java
package com.example;  
  
@Singleton  
public class RecommendatorImpl implements Recommendator {  
    @InjectProperty("wisky")  
    private String alcohol;  
  
    public RecommendatorImpl() {  
        System.out.println("RecommendatorImpl was created");  
    }  
  
    @Override  
    public void recommend() {  
        System.out.println("to protect from covid-2019 drink " + alcohol);  
    }  
}
```

```txt
RecommendatorImpl was created
Начинаем дезинфекцию, всё вон!
to protect from covid-2019 drink singleton
пиф паф, бах бах, кыш, кыш!
зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад
Рискните зайти обратно
to protect from covid-2019 drink singleton

Process finished with exit code 0
```

**Поговорим про следующую проблему**

У нас возникает следующая проблема: если мы возьмём полицейского и допустим он хочет у себя в конструкторе распечатать для логов какая имплементация для него заинжекталась:

```java
package com.example;  
  
public class PolicemanImpl implements Policeman {  
  
    @InjectByType  
    private Recommendator recommendator;  
      
    public PolicemanImpl() {  
        System.out.println(recommendator.getClass());  
    }  
  
    @Override  
    public void makePeopleLeaveRoom() {  
        System.out.println("пиф паф, бах бах, кыш, кыш!");  
    }  
}
```

Запускаем и видим ошибку:

```txt
RecommendatorImpl was created
Exception in thread "main" java.lang.reflect.InvocationTargetException
	at java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
	at java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:62)
	at java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
	at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:490)
	at com.example.ObjectFactory.createObject(ObjectFactory.java:24)
	at com.example.ApplicationContext.getObject(ApplicationContext.java:36)
	at com.example.InjectByTypeAnnotationObjectConfigurator.configure(InjectByTypeAnnotationObjectConfigurator.java:17)
	at com.example.ObjectFactory.lambda$createObject$0(ObjectFactory.java:27)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1541)
	at com.example.ObjectFactory.createObject(ObjectFactory.java:27)
	at com.example.ApplicationContext.getObject(ApplicationContext.java:36)
	at com.example.App.main(App.java:12)
Caused by: java.lang.NullPointerException
	at com.example.PolicemanImpl.<init>(PolicemanImpl.java:9)
	... 12 more

Process finished with exit code 1
```

У нас `NullPointerException` в конструкторе. Почему это происходит?

Всё на самом деле просто и очевидно. У нас метод `createObject()` состоит из нескольких этапов:

```java
    @SneakyThrows  
    public <T> T createObject(Class<T> implClass) {  
        T t = implClass.getDeclaredConstructor().newInstance();  
  
//        берём все наши конфигураторы и просим каждого из них настроить наш объект  
        configurators.forEach(objectConfigurator -> objectConfigurator.configure(t, context));  
  
        return t;  
    }
```

Вынесем этапы в отдельные методы:

```java
package com.example;  
  
import lombok.SneakyThrows;  
  
import java.lang.reflect.InvocationTargetException;  
import java.util.ArrayList;  
import java.util.List;  
  
public class ObjectFactory {  
    private final ApplicationContext context;  
    private List<ObjectConfigurator> configurators = new ArrayList<>();  
  
  
    @SneakyThrows  
    public ObjectFactory(ApplicationContext context) {  
        this.context = context;  
//        берём все подвиды нашего ObjectConfigurator'ов из контекста  
        for (Class<? extends ObjectConfigurator> aClass : context.getConfig().getScanner().getSubTypesOf(ObjectConfigurator.class)) {  
            configurators.add(aClass.getDeclaredConstructor().newInstance());  
        }  
    }  
  
    @SneakyThrows  
    public <T> T createObject(Class<T> implClass) {  
        T t = create(implClass);  
          
        configure(t);  
          
        return t;  
    }  
  
    private <T> void configure(T t) {  
        configurators.forEach(objectConfigurator -> objectConfigurator.configure(t, context));  
    }  
  
    private static <T> T create(Class<T> implClass) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {  
        return implClass.getDeclaredConstructor().newInstance();  
    }  
}
```

Первый этап - это создание объекта.

Второй этап - это его настройка

На этапе `create()` вызывается конструктор. Поскольку наш фреймворк не использует подход constructor-injection т.е. все наши dependency мы инжектим не в конструктор, а мы его потом сетим по филдам, то соответственно на том этапе когда работает конструктор ещё ничего не проинжектилось.

И соответственно когда в конструкторе пишем какой-то код который пытается работать с ещё не заинжекченым объектом. *Нельзя воспитывать ребенка который ещё не родился*. Нельзя настраивать объект пока он ещё не создался. 

**Что мы можем сделать.**

В мире инверсий контроля довольно редко будут возникать ситуации что мы в конструкторе будем писать какой-то код потому что в конструкторе пишут код который настраивает объект, а наш объект настраивается фреймворком нашим (нашим контекстом, фабрикой, конфигуратором), но не исключена вероятность что люди иногда будут что-то поднастраивать что нужно исключительно для них чем фреймворк не занимается.

*Поэтому нам надо отказаться* в таких случаях от использования конструктора и **будем использовать `init()` метод**. 

Проблема в том что `init()` метод никто нам не запустит и таким образом мы подходим к новой концепции **SECOND FACE CONSTRUCTOR**. Т.е. вторичный конструктор.

В мире инверсии контроля есть такая концепция которая говорит о том что у нас может быть `init()` код который надо прогнать после того как объект уже настроен. Не только когда он создан (замечание: конструктор не создаёт объект, а инициализирует его), а когда он уже настроен. И для этого уже даже придумали конвенции. Все такие методы должны помечаться аннотацией `@PostConstruct`. 

Проблема в том что на данный момент у нас не поддерживается нашим фреймворком поддержка аннотации `@PostConstruct` поэтому идём в нашу `ObjectFactory` и говорим: "после того как ты сконфигурировал объект, давай ка запусти у него инит-методы если они у него его".  Для этого берём наш `implClass` , берём все его методы, итерируемся и если метод аннотирован аннотацией `@PostConscruct`, то этот метод мы хотим запустить:

```java
package com.example;  
  
import lombok.SneakyThrows;  
  
import javax.annotation.PostConstruct;  
import java.lang.reflect.InvocationTargetException;  
import java.lang.reflect.Method;  
import java.util.ArrayList;  
import java.util.List;  
  
public class ObjectFactory {  
    private final ApplicationContext context;  
    private List<ObjectConfigurator> configurators = new ArrayList<>();  
  
  
    @SneakyThrows  
    public ObjectFactory(ApplicationContext context) {  
        this.context = context;  
//        берём все подвиды нашего ObjectConfigurator'ов из контекста  
        for (Class<? extends ObjectConfigurator> aClass : context.getConfig().getScanner().getSubTypesOf(ObjectConfigurator.class)) {  
            configurators.add(aClass.getDeclaredConstructor().newInstance());  
        }  
    }  
  
    @SneakyThrows  
    public <T> T createObject(Class<T> implClass) {  
        T t = create(implClass);  
  
        configure(t);  
  
        invokeInit(implClass, t);  
  
        return t;  
    }  
  
    private static <T> void invokeInit(Class<T> implClass, T t) throws IllegalAccessException, InvocationTargetException {  
        for (Method method : implClass.getMethods()) {  
            if (method.isAnnotationPresent(PostConstruct.class)) {  
                method.invoke(t);  
            }  
        }  
    }  
  
    private <T> void configure(T t) {  
        configurators.forEach(objectConfigurator -> objectConfigurator.configure(t, context));  
    }  
  
    private static <T> T create(Class<T> implClass) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {  
        return implClass.getDeclaredConstructor().newInstance();  
    }  
}
```

**Где запускать init методы `@PostConstruct`?**

1. Оставить в фабрике (как у нас)
2. Перенесём в ещё один конфигуратор (как в спринге)
3. Создадим новый тип конфигураторов

Single Responsibility у фабрики это создавать объект. Что такое создавать объект? Это запускать его конструктор (напоминание: создает объект JVM, а не конструктор. Конструктор инициализирует объект). Что такое метод помеченный `@PostConstruct`? Это вторичный конструктор и тоже как бы часть конструктора. Поэтому автор не видит причины по которой надо это выносить в какой-то конфигуратор потому что конфигураторов может быть много разных, но конфигураторов которые будут заниматься вызыванием `init` методов - он явно будет такой один. Поэтому никакой новый тип создавать ради этого создавать не имеет смысл, а засовывать это в конфигуратор будет очень серьезная проблема.

По поводу проблемы. Мы сказали о том, что очень важно чтобы мы `init` метод запускали после того как объект уже настроен. Соответственно если тот кто будет отвечать за запуск `init()` метода - это ещё один из конфигураторов, то он обязательно должен быть последним. А мы же не хотим приходить к ситуации когда у нас ordering становится важным.

В Spring обработка аннотации `@PostConstuct` вынесена в `BeanPostProcessor`, т.е. аналог нашего `ObjectConfigurator`, но с самым последним порядком так чтобы выполнялся в конце.

Мы не будем заморачиваться. Мы говорим простую вещь: фабрика отвечает за создание объекта, нет проблем чтобы фабрика дернула вызов `init()` метода потому что это как продолжение конструктора, а у нас нет никаких проблем что фабрика дергает конструктор.

Изменим `PolicemanImpl`, запустим и проверим что все работает теперь:

```java
package com.example;  
  
import javax.annotation.PostConstruct;  
  
public class PolicemanImpl implements Policeman {  
  
    @InjectByType  
    private Recommendator recommendator;  
  
    public PolicemanImpl() {  
//        System.out.println(recommendator.getClass());  
    }  
  
    @PostConstruct  
    public void init() {  
        System.out.println(recommendator.getClass());  
    }  
  
    @Override  
    public void makePeopleLeaveRoom() {  
        System.out.println("пиф паф, бах бах, кыш, кыш!");  
    }  
}
```

```txt
RecommendatorImpl was created
class com.example.RecommendatorImpl
Начинаем дезинфекцию, всё вон!
to protect from covid-2019 drink singleton
пиф паф, бах бах, кыш, кыш!
зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад
Рискните зайти обратно
to protect from covid-2019 drink singleton

Process finished with exit code 0
```

**Последняя тема. Прокси**

У нас `RecommendationImpl` и мы хотим пометить его аннотацией `@Deprecated`. Я хочу чтобы каждый раз когда будет работать метод деприкейтного класса - чтобы в лог печаталось уведомление.

Если посмотреть на нашу инфраструктуру которая умеет поддерживать синглтоны, она умеет делать следующее: она умеет создавать объект, потом его настраивает, потом вызывает `init()` метод который донастраивает его и вызывает настроенный объект.

Но мы хотим сделать кое-что ещё. Мы хотим дать возможность людям (в данном случае на примере аннотации `@Deprecated`) менять поведение работы методов. Т.е. я не хочу уничтожить уникальную логику. У меня метод `recommend()` в `RecommendatorImpl` как работал так и будет работать, но я хочу чтобы в дополнение ко всему печаталась перед началом его работы информация о том, что мы пользуемся методом deprecated класса. 

Разница между тем что мы хотим сделать с `@PostConstruct` в том что в `@PostConstruct` была необходимость вызывать один единственный раз вызвать `init()` метод. А здесь мы хотим изменить поведение методов класса и хотим печать в логи информацию о том что у нас запускается метод депрекейтного класса.

Тут у нас возникает следующий вопрос: *как добавить что-то в объект - понятно, а в метод как?*

Инструментировать код очень не хочется потому что на данный момент наш фреймворк реализован в рантайме. Всю магию которую он делает - делает в рантайме и мы не хотим сейчас отдельно подключаться на бутстрапе или на этапе компиляции и как-то начинать аспектировать наш класс. Поэтому будем пользоваться динамическим **Proxy Pattern**'ом (Подмена объекта на другой соответствующий по типу)

**Идея прокси паттерна** говорит следующее: если мы хотим засунуть какую-нибудь дополнительную логику в наш метод и при этом не имеем право писать здесь никакой код потому что это например деплой-класс. Мы можем подменить объект на другой объект который будет соответствовать по типу первого объекта. Т.е. если у нас есть `RecommendatorImpl`, то мы его заменим на какой-то `RecommendatirShmimpl`, но он будет все равно имплементировать тот же самый интерфейс поэтому его подмену никто не заметит и соответственно наша фабрика вместо того чтобы вернуть `RecommendatorImpl` - вернёт какой-то объект который называется `Proxy`.

У нас есть API в Java встроенный который называется `dynamic proxy` и есть библиотека `cglib` которая дает возможность делать прокси при помощи наследования.

**Как работает этот динамический прокси.**

Представьте что у нас есть какой-то интерфейс `ifc` у которого есть метод `a()` и метод `b(int)` который принимает интеджер. И у нас есть какая-то наша имплементация которая называется `ifcImpl` у которой тоже есть метод `a()` и `b(int)`. Библиотека которая называется `dynamic proxy` которая существует с 1999 года и даёт возможность создать налету прокси-класс и рассказать какие интерфейсы этот прокси-класс должен имплементировать придет к тому что у нас создатся `ProxyClass` у которого тоже будут методы `a()` и `b(int)` (создастся налету).

Посмотрим как он генерится, а потом поговорим о том что именно он делает.\

Идём в наш `ObjectFactory` в `createObject()`. У класса `Proxy` вызываем `newProxyInstance()` (по названию метода понятно что он не только сгенерит класс на ходу, но ещё и объект из него создаст).

В классе который сгенерится налету (`ProxyClass`) который на самом деле даже пофиг как он называеся тоже будет метод `a()` и тоже будет метод `b(int)`.

Первый параметр который должны передать в `newProxyInstance()` это `implClass.getClassLoader()` через который этот класс загрузится, а второй параметр очень интересный. Это интерфейсы которые должен имплементировать класс который сгенерят налету. И соответственно методы которые будут у нас в прокси-классе это тоже метод `a()` и метод `b(int)`. Если в методе `a()` и методе `b(int)` у нашей имплементации `ifcImpl` не возникает вопроса "что там за код написан", то в `ProxyClass` возникает очень серьезный вопрос: "мне на лету сгенерили класс, у него есть метод `a()` и `b(int)` потому то сказали какой интерфейс надо имплементировать. Но что там за код будет написан?"

Дело в том что когда создаём прокси-объект - мы третьим параметром должны передавать такую штуку как `InvocationHandler`. Для примера создадим просто анонимный объект у которого есть метод `invoke(Method, Args)` который принимает методы и аргументы. И как реализован метод `a()` который для нас создала java и как реализован метод `b(int)` - они реализованы абсолютно одинаково. Прокси-класс - это самый тупой класс который только можно придумать - он ничего не умеет делать. Единственное то он умеет делать - это бежать в `InvocationHandler` который к нему присобачен и кричать "у меня вызвали метод `a()`, смотри, никаких аргументов не передали! что делать?", "о, у меня вызвали метод `b(int)`! туда передали 12! что делаем?" и соответственно `InvocationHandler` что-то должен делать. А что он будет делать? 

Во-первых `InvocationHandler` мы пишем сами. Поэтому будем запускать ту логику которую мы хотим. Например будем выводить текст.

```java
@SneakyThrows  
public <T> T createObject(Class<T> implClass) {  
    T t = create(implClass);  
  
    configure(t);  
  
    invokeInit(implClass, t);  
  
    Proxy.newProxyInstance(implClass.getClassLoader(), implClass.getInterfaces(), new InvocationHandler() {  
        @Override  
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {  
            System.out.println("******* что ж ты делаешь урод!!");  
              
        }  
    });  
  
    return t;  
}
```

После того как мы сделали дополнительную логику - мы рано или поздно хотим вызвать настоящую логику нашего класса. В нашем случае это `RecommendatorImpl`.

Как мы это делаем? После того (или до) как наша логика в `invoke()` отработала - мы запускаем соответствующий метод либо `a()` либо `b(int)`  на настоящей имплементации `ifcImpl`.

Метод `invoke(Object proxy, Method method, Object[] args)` получает информацию о том какой метод запустился (аргумент `Method method`) у прокси и мы тот же самый метод запустим у нашего настоящего метода `t`. НЕ ЗАПУСКАЕМ `Object proxy` поскольку это будет бесконечная рекурсия (прокси класс вызывает invoke, а invoke будет дергать метод из прокси и так до бесконечности).

```java
@SneakyThrows  
public <T> T createObject(Class<T> implClass) {  
    T t = create(implClass);  
  
    configure(t);  
  
    invokeInit(implClass, t);  
  
    if (implClass.isAnnotationPresent(Deprecated.class)) {  
        return (T) Proxy.newProxyInstance(implClass.getClassLoader(), implClass.getInterfaces(), new InvocationHandler() {  
            @Override  
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {  
                System.out.println("******* что ж ты делаешь урод!!");  
  
                return method.invoke(t);  
            }  
        });  
    }  
  
    return t;  
}
```

![[Pasted image 20241008030556.png]]

Запустим и проверим что все работает:

```java
package com.example;  
  
@Deprecated  
@Singleton  
public class RecommendatorImpl implements Recommendator {  
    @InjectProperty("wisky")  
    private String alcohol;  
  
    public RecommendatorImpl() {  
        System.out.println("RecommendatorImpl was created");  
    }  
  
    @Override  
    public void recommend() {  
        System.out.println("to protect from covid-2019 drink " + alcohol);  
    }  
}
```

```java
package com.example;  
  
import lombok.SneakyThrows;  
  
import javax.annotation.PostConstruct;  
import java.lang.reflect.InvocationHandler;  
import java.lang.reflect.InvocationTargetException;  
import java.lang.reflect.Method;  
import java.lang.reflect.Proxy;  
import java.util.ArrayList;  
import java.util.List;  
  
public class ObjectFactory {  
    private final ApplicationContext context;  
    private List<ObjectConfigurator> configurators = new ArrayList<>();  
  
  
    @SneakyThrows  
    public ObjectFactory(ApplicationContext context) {  
        this.context = context;  
//        берём все подвиды нашего ObjectConfigurator'ов из контекста  
        for (Class<? extends ObjectConfigurator> aClass : context.getConfig().getScanner().getSubTypesOf(ObjectConfigurator.class)) {  
            configurators.add(aClass.getDeclaredConstructor().newInstance());  
        }  
    }  
  
    @SneakyThrows  
    public <T> T createObject(Class<T> implClass) {  
        T t = create(implClass);  
  
        configure(t);  
  
        invokeInit(implClass, t);  
  
        if (implClass.isAnnotationPresent(Deprecated.class)) {  
            return (T) Proxy.newProxyInstance(implClass.getClassLoader(), implClass.getInterfaces(), new InvocationHandler() {  
                @Override  
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {  
                    System.out.println("******* что ж ты делаешь урод!!");  
  
                    return method.invoke(t);  
                }  
            });  
        }  
  
        return t;  
    }  
  
    private static <T> void invokeInit(Class<T> implClass, T t) throws IllegalAccessException, InvocationTargetException {  
        for (Method method : implClass.getMethods()) {  
            if (method.isAnnotationPresent(PostConstruct.class)) {  
                method.invoke(t);  
            }  
        }  
    }  
  
    private <T> void configure(T t) {  
        configurators.forEach(objectConfigurator -> objectConfigurator.configure(t, context));  
    }  
  
    private static <T> T create(Class<T> implClass) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {  
        return implClass.getDeclaredConstructor().newInstance();  
    }  
}
```

```txt
RecommendatorImpl was created
class com.sun.proxy.$Proxy6
Начинаем дезинфекцию, всё вон!
******* что ж ты делаешь урод!!
to protect from covid-2019 drink singleton
пиф паф, бах бах, кыш, кыш!
зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад
Рискните зайти обратно
******* что ж ты делаешь урод!!
to protect from covid-2019 drink singleton

Process finished with exit code 0
```

Следует обратить внимание, что теперь вместо `RecommendatorImpl` пишется `Proxy6`

Чтобы было совсем понятно давайте посмотрим что у нас получается. 

Кто у нас запрашивает объекты от нашего фреймворка? Опять же у нас есть два варианта - либо мы делаем lookup (обычно мы его не делаем). Lookup делается в худшем случае в одном единственном месте это в нашем `main()` это когда мы говорим: "контекст, дай мне `CoronaDesinfector`" `context.getObject(CoronaDesinfector.class)`. Т.е. lookup можем привести к тому чтобы создать объект или аннотация `@InjectByType` может спровоцировать наш конфигуратор и он пойдёт в контекст и скажет "чувак, мне нужен объект подходящий под этот тип". Контекст пойдёт в фабрику и скажет: "фабрика, нужно создать объект подходящий под этот тип". А фабрика возьмет и вернёт обратно не `RecommendatorImpl`, а она вернёт соответственно proxy. 

![[Pasted image 20241008043229.png]]

У нас получается, что наш `AnnounerImpl` который пользуется `RecommendationImpl` - он раньше вызывал `recommend()` у `RecommendationImpl`. Теперь механизм injection который обратился к фабрике вытащил объект `Proxy`. Т.е. наш `AnnouncerImpl` работает уже не с `RecommendationImpl`, а с `Proxy` которая делегирует любой вызов в `InvocationHandler` который писали мы со всей этой магией, который в конечном итоге вызывает уже тот же самый `RecommendationImpl` который раньше был в самом начале. Т.е. удлиняется просто цепочка вызовов, мы теряем 4,5нс на перфомансе, но зато у нас получается типа АОП, типа возможность делать какие-то дополнительные штуки до вызова метода или после или вместо вызова метода.

Т.е. мы можем написать механизм секьюрити который например вообще не вызывал бы настоящий метод.

**Куда вынесем логику обработки `@Deprecated`?**

1. Оставим в фабрике
2. Перенесём в ещё один конфигуратор (спринговый способ)
3. Создадим новый тип конфигураторов (как сделаем)

Причина по которой в спринге иногда довольно странные решения заключается в том что в 2002 когда фреймворк разрабатывался - дизайн паттерны ещё не настолько было приняты поэтому писали как писали, а сегодня уже работает - не трогай.

По мимо этого есть ещё странные решения - почему в `BeanPostProcessor` два метода, почему за аннотацию `@PostConstruct` отвечает `BeanPostProcessor`, почему ordering такой сложный и хитрый. 

Поэтому мы делаем более правильно. Почему это не может быть ещё один конфигуратор типа `ObjectConfigurator`? Во первых потому что у нас совершенно неправильная сигнатура. Если посмотреть на наш `ObjectConfigurator`, то мы видим что метод `void configure(Object t, ApplicationContext context)` ничего не возвращает

```java
package com.example;  
  
public interface ObjectConfigurator {  
    void configure(Object t, ApplicationContext context);  
}
```

 А сейчас же мы должны возвращать прокси. Это раз. Можно конечно сказать: "а давайте возвращать `Object`". Но это будет бред и люди будут путаться потому что когда конфигуратор настраивает объект, когда он должен заменять его на проксю. Поэтому что сделал спринг - спринг сделал ещё один метод который возвращает что-то. 
 
 А мы сделаем новый интерфейс потому что четвертое правило из SOLID говорит нам о том что не надо делать слишком толстые интерфейсы, а надо делать тонкие интерфейсы. Поэтому мы берём и создаём класс `ProxyConfigurator` и всякие разные штуки типа транзакций, асинхронных методов где надо логику методов подменять - мы будем делать с его помощью

```java
package com.example;  
  
public interface ProxyConfigurator {  
    //    необязательно возвращает проксю. например может просто не стоять аннотация @Deprecated  
    Object replaceWithProxyIfNeeded(Object t, Class implClass);  
}
```

Необязательно вернётся прокся. Если у человека нет @Deprecated методов, нет транзакций, нет каких-то других вещей которые приведут к тому что нужно дополнительную логику вызывать до или после оригинальных методов, то зачем прокся? Мы не делаем проксю для всех.

Нам будет приходить объект `Object t` который надо сконфигурировать и будем передавать класс имплементации. Зачем мы передаём `Class implClass` если мы можем сделать `t.getClass()` и получить? На этапе работы конфигураторов в `createObject()` которые по цепочке конфигурируют объекты 

```java
private <T> void configure(T t) {  
    configurators.forEach(objectConfigurator -> objectConfigurator.configure(t, context));  
}
```

```java
    @Override  
    @SneakyThrows    
    public void configure(Object t, ApplicationContext context) {  
        Class<?> implClass = t.getClass();  
        for (Field field : implClass.getDeclaredFields()) {  
            InjectProperty annotation = field.getAnnotation(InjectProperty.class);  
  
            if (annotation != null) {  
                String value;  
                if (annotation.value().isEmpty()) {  
                    value = propertiesMap.get(field.getName());  
                } else {  
                    value = propertiesMap.get(annotation.value());  
                }  
  
                field.setAccessible(true);  
//                настраиваем объект t  
                field.set(t, value);  
            }  
        }  
    }
```

каждый конфигуратор начинает с того что он смотрит с какого класса объект (`Class<?> implClass = t.getClass()`) чтобы посмотреть филды, методы и т.д. Но когда  мы будем проходиться по цепочке прокси-конфигураторов - каждый из прокси конфигураторов может вернуть проксю. И может быть прокся в проксе. И соответственно если мы будем ориентироваться на вызов метода `t.getClass()`, то есть вероятность что у нас придет прокся у которой нет никаких аннотаций которые были в оригинальном классе. Поэтому лучше этот `implClass` прокидывать изначально. 

В спринге это сделали по другому. Там это по другому можно реализовать, там инжектить надо что-то но не суть.

Сделаем последний на сегодня класс `DeprecatedHandlerProxyConfigurator`. В нём поставим логику проксирования которая была в `createObject`

```java
package com.example;  
  
import java.lang.reflect.InvocationHandler;  
import java.lang.reflect.Method;  
import java.lang.reflect.Proxy;  
  
public class DeprecatedHandlerProxyConfigurator implements ProxyConfigurator {  
    @Override  
    public Object replaceWithProxyIfNeeded(Object t, Class implClass) {  
        if (implClass.isAnnotationPresent(Deprecated.class)) {  
            return Proxy.newProxyInstance(implClass.getClassLoader(), implClass.getInterfaces(), new InvocationHandler() {  
                @Override  
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {  
                    System.out.println("******* что ж ты делаешь урод!!");  
  
                    return method.invoke(t);  
                }  
            });  
        } else {  
            return t;  
        }  
    }  
}
```

Т.е. ещё раз. Конфигуратор `DeprecatedHandlerProxyConfigurator` принимает объект `Object t` который надо возможно запроксировать. Он говорит: "я отвечаю за проксирование в случае если класс `Deprecated`". Если у класса есть аннотация `@Deprecated`, то будем возвращать проксю которая работает с `InvocationHandler` и в конечном итоге оригинальные методы будут вызываться, но сначала проходим через дополнительную нашу логику. А если не было аннотации `@Deprecated`, то ничего не надо делать и просто возвращаем тот же самый объект который получили.

Возвращаемся в `ObjectFactory` и обучаем его работать с прокси-конфигуратором. У нас будет список `List<ProxyConfigurator> proxyConfigurators`. Точно также как находили все `ObjectConfigurator` - также будем находить все `ProxyConfigurator` в нашем пакете classpath.

```java
package com.example;  
  
import lombok.SneakyThrows;  
  
import javax.annotation.PostConstruct;  
import java.lang.reflect.InvocationTargetException;  
import java.lang.reflect.Method;  
import java.util.ArrayList;  
import java.util.List;  
  
public class ObjectFactory {  
    private final ApplicationContext context;  
    private List<ObjectConfigurator> configurators = new ArrayList<>();  
    private List<ProxyConfigurator> proxyConfigurators = new ArrayList<>();  
  
  
    @SneakyThrows  
    public ObjectFactory(ApplicationContext context) {  
        this.context = context;  
//        берём все подвиды нашего ObjectConfigurator'ов из контекста  
        for (Class<? extends ObjectConfigurator> aClass : context.getConfig().getScanner().getSubTypesOf(ObjectConfigurator.class)) {  
            configurators.add(aClass.getDeclaredConstructor().newInstance());  
        }  
  
//        берём все прокси-конфигураторі из контекста, а именно из класспаса  
        for (Class<? extends ProxyConfigurator> aClass : context.getConfig().getScanner().getSubTypesOf(ProxyConfigurator.class)) {  
            proxyConfigurators.add(aClass.getDeclaredConstructor().newInstance());  
        }  
    }  
  
    @SneakyThrows  
    public <T> T createObject(Class<T> implClass) {  
        T t = create(implClass);  
  
        configure(t);  
  
        invokeInit(implClass, t);  
  
        t = wrapWithProxyIfNeeded(implClass, t);  
  
        return t;  
    }  
  
    private <T> T wrapWithProxyIfNeeded(Class<T> implClass, T t) {  
        for (ProxyConfigurator proxyConfigurator : proxyConfigurators) {  
//            дадим возможность каждому из прокси-конфигураторов заменить объект  
            t = (T) proxyConfigurator.replaceWithProxyIfNeeded(t, implClass);  
        }  
        return t;  
    }  
  
    private static <T> void invokeInit(Class<T> implClass, T t) throws IllegalAccessException, InvocationTargetException {  
        for (Method method : implClass.getMethods()) {  
            if (method.isAnnotationPresent(PostConstruct.class)) {  
                method.invoke(t);  
            }  
        }  
    }  
  
    private <T> void configure(T t) {  
        configurators.forEach(objectConfigurator -> objectConfigurator.configure(t, context));  
    }  
  
    private static <T> T create(Class<T> implClass) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {  
        return implClass.getDeclaredConstructor().newInstance();  
    }  
}
```

Запускаем и проверяем что оно по прежнему работает:

```txt
RecommendatorImpl was created
class com.sun.proxy.$Proxy6
Начинаем дезинфекцию, всё вон!
******* что ж ты делаешь урод!!
to protect from covid-2019 drink singleton
пиф паф, бах бах, кыш, кыш!
зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад
Рискните зайти обратно
******* что ж ты делаешь урод!!
to protect from covid-2019 drink singleton

Process finished with exit code 0
```

**Последнюю вещь которую посмотрим - это что будет если у нас будет `@Deprecated` метод в классе у которого нет интерфейса.**

```java
package com.example;  
  
@Deprecated  
public class CoronaDesinfector {  
  
    @InjectByType  
    private Announcer announcer;  
    @InjectByType  
    private Policeman policeman;  
  
    public void start(Room room) {  
        announcer.announce("Начинаем дезинфекцию, всё вон!");  
        policeman.makePeopleLeaveRoom();  
        desinfect(room);  
        announcer.announce("Рискните зайти обратно");  
    }  
  
    private void desinfect(Room room) {  
        System.out.println("зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад");  
    }  
}
```

Если мы запустим, то получим ошибку:

```txt
RecommendatorImpl was created
class com.example.RecommendatorImpl
Exception in thread "main" java.lang.ClassCastException: class com.sun.proxy.$Proxy7 cannot be cast to class com.example.CoronaDesinfector (com.sun.proxy.$Proxy7 and com.example.CoronaDesinfector are in unnamed module of loader 'app')
	at com.example.App.main(App.java:12)

Process finished with exit code 1
```

В ошибке указано, что он не может закастить класс который `$Proxy7` в `CoronaDesinfector` потому что раз у него стоит `@Deprecated`, то он обязательно попадёт в

```java
package com.example;  
  
import java.lang.reflect.InvocationHandler;  
import java.lang.reflect.Method;  
import java.lang.reflect.Proxy;  
  
public class DeprecatedHandlerProxyConfigurator implements ProxyConfigurator {  
    @Override  
    public Object replaceWithProxyIfNeeded(Object t, Class implClass) {  
        if (implClass.isAnnotationPresent(Deprecated.class)) {  
            return Proxy.newProxyInstance(implClass.getClassLoader(), implClass.getInterfaces(), new InvocationHandler() {  
                @Override  
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {  
                    System.out.println("******* что ж ты делаешь урод!!");  
  
                    return method.invoke(t);  
                }  
            });  
        } else {  
            return t;  
        }  
    }  
}
```

а именно во внутрь `if (implClass.isAnnotationPresent(Deprecated.class))` где дальше в `Proxy.newProxyInstance(implClass.getClassLoader(), implClass.getInterfaces(), new InvocationHandler() {` у него спрашивают - какие у него есть интерфейсы. Он говорит, что никаких интерфейсов у него нет. А прокси говорит отлично: "мы создадим класс который не имплементирует никакие интерфейсы". Т.е. создаётся класс у которого нет методов. Но упало это ещё раньше потому что класс в который мы пытаемся закастить (мы же в `main()` пытаемся закастить до `CoronaDesinfector`, а получили какой-то непонятный `$Proxy7` который вообще не имеет ничего общего с `CoronaDesinfector`)

```java
package com.example;  
  
import java.util.HashMap;  
import java.util.Map;  
  
public class App {  
    public static void main(String[] args) {  
        ApplicationContext context = ApplicationRunner.run(  
                "com.example",  
                new HashMap<>(Map.of(Policeman.class, PolicemanImpl.class))  
        );  
        CoronaDesinfector desinfector = context.getObject(CoronaDesinfector.class);  
        desinfector.start(new Room());  
    }  
}
```

вот если бы прокси-класс создался бы не при помощи интерфейса, а при помощи того что он отнаследовался от этой штуки, то тогда было бы все хорошо.

Сделаем это. Добавим библиотеку `cglib`

```xml
<dependency>  
    <groupId>cglib</groupId>  
    <artifactId>cglib</artifactId>  
    <version>3.3.0</version>  
</dependency>
```

которая предоставляет похожий API прокси с помощью которого можно соотвественно через наследование проксироваться. Поэтому мы вводим дополнительное условие, что если у нас нет интерфейсов, то нам нужно делать return не при помощи `Proxy.newProxyInstace`, а при помощи `Enhancer.create` который из `cglib`.

```java
package com.example;  
  
import net.sf.cglib.proxy.Enhancer;  
import net.sf.cglib.proxy.InvocationHandler;  
  
import java.lang.reflect.InvocationTargetException;  
import java.lang.reflect.Method;  
import java.lang.reflect.Proxy;  
  
public class DeprecatedHandlerProxyConfigurator implements ProxyConfigurator {  
    @Override  
    public Object replaceWithProxyIfNeeded(Object t, Class implClass) {  
        if (implClass.isAnnotationPresent(Deprecated.class)) {  
  
            if (implClass.getInterfaces().length == 0) {  
                return Enhancer.create(implClass, (InvocationHandler) (proxy, method, args) -> getInvocationHandler(t, method, args));  
            }  
  
            return Proxy.newProxyInstance(implClass.getClassLoader(), implClass.getInterfaces(), (proxy, method, args) -> getInvocationHandler(t, method, args));  
        } else {  
            return t;  
        }  
    }  
  
    private static Object getInvocationHandler(Object t, Method method, Object[] args) throws IllegalAccessException, InvocationTargetException {  
        System.out.println("******* что ж ты делаешь урод!!");  
        return method.invoke(t, args);  
    }  
}
```

Попробуем запустить

```txt
RecommendatorImpl was created
class com.example.RecommendatorImpl
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by net.sf.cglib.core.ReflectUtils$1 (file:/C:/Users/kainv/.m2/repository/cglib/cglib/3.3.0/cglib-3.3.0.jar) to method java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int,java.security.ProtectionDomain)
WARNING: Please consider reporting this to the maintainers of net.sf.cglib.core.ReflectUtils$1
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
******* что ж ты делаешь урод!!
Начинаем дезинфекцию, всё вон!
to protect from covid-2019 drink singleton
пиф паф, бах бах, кыш, кыш!
зачитывается молитва: 'корона изыди!' - молитва прочитана, вирус низвергнут в ад
Рискните зайти обратно
to protect from covid-2019 drink singleton

Process finished with exit code 0
```

Видим что всё отлично отработало и у нас рекомендатор вернулся потому что мы сняли с него `@Deprecated`, но мы добавили `@Deprecated` на `CoronaDesinfector`.

Логика `InvocationHandler` необязательно должна быть тупая как у нас. А теперь представьте если бы мы хотели чтобы у нас не класс был `@Deprecated`, а например метод `start()` был `@Deprecated`. Я хотел бы чтобы если его вызывают - то печатать сообщение что deprecated метод вызывается, а если не deprecated метод, то соответственно не печатать. Как это достигнуть? На данный момент наш `InvocationHandler` тупой. Когда к нему приходят и говорят: "что делать?" он говорит: "я знаю что делать!" - сказать что все уроды, а потом вызвать оригинальный код. А по хорошему он должен был бы сначала проверять если тот метод который к нему обратился хочет депрекейтед, то тогда соответственно пожалуйста. Если не хочет, то не нужно.

**Финальная картина**

```java
@SneakyThrows  
public <T> T createObject(Class<T> implClass) {  
    T t = create(implClass);  
  
    configure(t);  
  
    invokeInit(implClass, t);  
  
    t = wrapWithProxyIfNeeded(implClass, t);  
  
    return t;  
}
```

Вот так работает спринг и это основные этапы которые есть. На этом слайде мы не видим все что связано с контекстом - что там кешируется, что там резолвится имплементация, что идёт работа с конфигурацией. 

Тут мы видим исключительно как уже строятся объекты. Сначала он просто создаётся. При чем в спринге он может ещё создаваться так что в конструктор засунутся какие-то вещи которые надо заинжектить. 

Потом он в любом случае настраивается всякими разными конфигураторами которые могут проинжектить в него всякие дополнительные вещи.

Потом запускается `invokeInit()` метод

И потом он проходит по цепоке прокси-конфигураторов которые могут его завернуть в проксю, а то и несколько слоёв. В спринге они обычно коллапсятся в один хотя в спринге бывают случаи когда прокся в проксе и в конечном итоге мы возвращаем объект который наш синглтон ещё и закеширует (т.е. наш контекст закеширует если это синглтон).