import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

interface Event{}

@interface Subscribe{}

interface EventHandler<T extends Event> 
{
    void handle(T event);
}

class EventBusImpl 
{
    private static EventBusImpl instance = null;
    private final HashMap<Class<?>, List<Subscription>> subscribers = new HashMap<>();

    private EventBusImpl() {}

    public static EventBusImpl getInstance() 
    {
        if(instance==null)
        {
            instance=new EventBusImpl();
        }
        return instance;
    }

    public void register(Object subscriber)
    {
        for(Method method : subscriber.getClass().getDeclaredMethods())
        {
            if(method.isAnnotationPresent(Subscribe.class))
            {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if(parameterTypes.length==1 && Event.class.isAssignableFrom(parameterTypes[0]))
                {
                    List<Subscription> subList = subscribers.get(parameterTypes[0]);
                    if(subList==null)
                    {
                        subList=new ArrayList<>();
                        subscribers.put(parameterTypes[0], subList);
                    }
                    subList.add(new Subscription(subscriber, method));
                }
            }
        }
    }

    public void post(Event event)
    {
        Class<?> eventType = event.getClass();
        List<Subscription> subList = new ArrayList<>();
        subList.addAll(subscribers.getOrDefault(eventType, new ArrayList<>()));

        for(Map.Entry<Class<?>, List<Subscription>> entry : subscribers.entrySet())
        {
            if(entry.getKey().isAssignableFrom(eventType) && !entry.getKey().equals(eventType))
            {
                subList.addAll(entry.getValue());
            }
        }
        
        if(subList!=null)
        {
            for(Subscription sub : subList)
            {
                if(sub.getMethod()!=null)
                {
                    try
                    {
                        sub.getMethod().setAccessible(true);
                        sub.getMethod().invoke(sub.getSubscriber(), event);
                    }
                    catch(IllegalAccessException | InvocationTargetException e)
                    {
                        e.printStackTrace();
                    }
                }
                else if(sub.getHandler()!=null)
                {
                    EventHandler<Event> handler=(EventHandler<Event>)sub.getHandler();
                    if(handler!=null)
                    {
                        handler.handle(event);
                    }
                }
            }
        }
        

        for(Map.Entry<Class<?>, List<Subscription>> entry : subscribers.entrySet())
        {
            if(entry.getKey().isAssignableFrom(eventType) && !entry.getKey().equals(eventType))
            {
                for(Subscription sub : entry.getValue())
                {
                    try
                    {
                        sub.getMethod().setAccessible(true);
                        sub.getMethod().invoke(sub.getSubscriber(), event);
                    }
                    catch(IllegalAccessException | InvocationTargetException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }

        System.out.println("Posting event: " + event.getClass().getName());
        System.out.println("Subscribers found: " + subList.size());
        for (Subscription sub : subList) 
        {
            System.out.println(" -> Notifying: " + sub.getSubscriber().getClass().getName());
        }
    }

    public <T extends Event> void registerHandle(Class<T> eventType, EventHandler<T> handler)
    {
        List<Subscription> subList = subscribers.get(eventType);
        if(subList==null)
        {
            subList=new ArrayList<>();
            subscribers.put(eventType, subList);
        }
        subList.add(new Subscription(handler));
        System.out.println("Registered via handler: " + handler.getClass().getName() + " for " + eventType.getName());
    }
}


class Subscription
{
    private Object subscriber;
    private Method method;
    private EventHandler<?> handler;

    public Subscription(Object subscriber, Method method)
    {
        this.subscriber = subscriber;
        this.method=method;
    }

    public <T extends Event> Subscription(EventHandler<T> handler)
    {
        this.handler=handler;
    }

    public Method getMethod()
    {
        return method;
    }

    public Object getSubscriber()
    {
        return subscriber;
    }

    public EventHandler<?> getHandler() 
    {
        return handler;
    }
}

class TemperatureSensor
{
    private final String id;
    private int temperature;
    private final Random random=new Random();

    public TemperatureSensor(String id)
    {
        this.id=id;
        this.temperature=random.nextInt(40);
    }

    public void generateTemperature()
    {
        this.temperature=random.nextInt(40);
        System.out.println("Sensor " + id + " - New Temperature: " + temperature);
        EventBusImpl.getInstance().post(new TemperatureEvent(this, temperature));
    }
}

class TemperatureEvent implements Event
{
    private final TemperatureSensor sensor;
    private final int temperature;

    public TemperatureEvent(TemperatureSensor sensor, int temperature)
    {
        this.sensor = sensor;
        this.temperature = temperature;
    }

    public TemperatureSensor getSensor()
    {
        return sensor;
    }

    public int getTemperature()
    {
        return temperature;
    }
}

class WaterLevelSensor
{
    private final String id;
    private int waterLevel;
    private final Random random= new Random();

    public WaterLevelSensor(String id)
    {
        this.id=id;
        this.waterLevel=random.nextInt(100);
    }

    public void generateWaterLevel()
    {
        this.waterLevel=random.nextInt(100);
        System.out.println("Sensor "+id+" - New Water Level: " + waterLevel);
        EventBusImpl.getInstance().post(new WaterLevelEvent(this, waterLevel));
    }
}

class WaterLevelEvent implements Event
{
    private final int waterLevel;
    private final WaterLevelSensor sensor;

    public WaterLevelEvent(WaterLevelSensor sensor, int waterLevel)
    {
        this.sensor=sensor;
        this.waterLevel=waterLevel;
    }

    public WaterLevelSensor getSensor()
    {
        return sensor;
    }

    public int getWaterLevel()
    {
        return waterLevel;
    }
}

class NewsAgency
{
    private final String name;
    private final Random random=new Random();

    public NewsAgency(String name)
    {
        this.name=name;
    }

    public void publishNews(String category)
    {
        String[] headlines={ "Breaking news!", "Big update!", "Shocking event!", "Important announcement!"};
        String newsContent=headlines[random.nextInt(headlines.length)];

        switch(category.toLowerCase())
        {
            case "sports": 
                EventBusImpl.getInstance().post(new SportsNewsEvent(this, newsContent));
                break;
            case "political": 
                EventBusImpl.getInstance().post(new PoliticalNewsEvent(this, newsContent));
                break;
            case "culture": 
                EventBusImpl.getInstance().post(new CultureNewsEvent(this, newsContent));
                break;
            default: 
                System.out.println("Unknown category");
        }

        System.out.println(getName() + " published " + category.toUpperCase() + " news: " + newsContent);
    }

    public String getName()
    {
        return name;
    }
}

abstract class NewsEvent implements Event
{
    private final NewsAgency agency;
    private final String newsContent;

    public NewsEvent(NewsAgency agency, String newsContent)
    {
        this.agency=agency;
        this.newsContent=newsContent;
    }

    public NewsAgency getAgency()
    {
        return agency;
    }

    public String getContent()
    {
        return newsContent;
    }
}

class SportsNewsEvent extends NewsEvent
{
    public SportsNewsEvent(NewsAgency agency, String newsContent)
    {
        super(agency, "[Sports] "+newsContent);
    }
}

class PoliticalNewsEvent extends NewsEvent
{
    public PoliticalNewsEvent(NewsAgency agency, String newsContent)
    {
        super(agency, "[Political] "+newsContent);
    }
}

class CultureNewsEvent extends NewsEvent
{
    public CultureNewsEvent(NewsAgency agency, String newsContent)
    {
        super(agency, "[Cultural] "+newsContent);
    }
}

class NumericDisplay
{
    private final String name;

    public NumericDisplay(String name)
    {
        this.name=name;
    }

    @Subscribe
    public void handleTemperatureEvent(TemperatureEvent event)
    {
        System.out.println(name + " - Temperature: " + event.getTemperature() + "°C");
    } 

    @Subscribe
    public void handleWaterEvent(WaterLevelEvent event)
    {
        System.out.println(name+" - Water Level: "+event.getWaterLevel());
    }
}

class TextDisplay
{
    private final String name;

    public TextDisplay(String name)
    {
        this.name=name;
    }

    @Subscribe
    public void handleWaterEvent(WaterLevelEvent event)
    {
        String text = event.getWaterLevel()<36 ?"All good ":"Run for your lives";
        System.out.println(name+" - "+text);
    }

    @Subscribe
    public void handleTemperatureEvent(TemperatureEvent event)
    {
        String text = event.getTemperature() <20 ? "Cold " :"Warm ";
        System.out.println(name+" - "+text);
    }
}

class HumanSubscriber
{
    private final String name;

    public HumanSubscriber(String name)
    {
        this.name=name;
    }

    @Subscribe
    public void handleSportsEvent(SportsNewsEvent event) 
    {
        System.out.println(name + " received sports news from " + event.getAgency().getName() + ": " + event.getContent());
    }

    @Subscribe
    public void handlePoliticalEvent(PoliticalNewsEvent event) 
    {
        System.out.println(name + " received political news from " + event.getAgency().getName() + ": " + event.getContent());
    }

    @Subscribe
    public void handleCultureEvent(CultureNewsEvent event) 
    {
        System.out.println(name + " received culture news from " + event.getAgency().getName() + ": " + event.getContent());
    }
}

public class ReflectedEventBus
{
    public static void main(String[] args) 
    {
        EventBusImpl eventBus = EventBusImpl.getInstance();

        NumericDisplay display1 = new NumericDisplay("Display 1");
        TextDisplay display2 = new TextDisplay("Display 2");
        HumanSubscriber human = new HumanSubscriber("Vasilica");

        TemperatureSensor tempSensor = new TemperatureSensor("T1");
        WaterLevelSensor waterSensor = new WaterLevelSensor("W1");
        NewsAgency newsAgency = new NewsAgency("ProTV");

        eventBus.register(display1);
        eventBus.register(display2);
        eventBus.register(human);

        eventBus.registerHandle(TemperatureEvent.class, event -> 
        {
            System.out.println("[Explicit] Handling temperature event: " + event.getTemperature());
        });

        eventBus.registerHandle(WaterLevelEvent.class, event -> 
        {
            System.out.println("[Explicit] Water level detected: " + event.getWaterLevel());
        });

        eventBus.post(new TemperatureEvent(tempSensor, 28));
        eventBus.post(new WaterLevelEvent(waterSensor, 50));

        eventBus.post(new SportsNewsEvent(newsAgency, "Epic sports victory!"));
        eventBus.post(new PoliticalNewsEvent(newsAgency, "Election updates!"));
        eventBus.post(new CultureNewsEvent(newsAgency, "New art exhibition!"));

        for (int i = 0; i < 5; i++) 
        {
            tempSensor.generateTemperature();
            waterSensor.generateWaterLevel();
            System.out.println();

            try 
            {
                Thread.sleep(100);
            } 
            catch (InterruptedException e) 
            {
                e.printStackTrace();
            }
        }

        System.out.println();
        newsAgency.publishNews("political");
        newsAgency.publishNews("sports");
        newsAgency.publishNews("culture");
    }
}
