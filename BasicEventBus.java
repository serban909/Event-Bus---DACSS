import java.util.*;

interface BasicBus
{
    void publish(Event event);
    void subscribe(Class<?> eventType, Subscriber subscriber);
    void unsubscribe(Class<?> eventType, Subscriber subscriber);
}

interface Event{}

interface Subscriber
{
    void inform(Event event);
}

class Subscription
{
    private Class<?> eventType;
    private Subscriber subscriber;

    public Subscription(Class<?> eventType, Subscriber subscriber)
    {
        this.eventType = eventType;
        this.subscriber = subscriber;
    }

    public Class<?> getEventType()
    {
        return eventType;
    }

    public Subscriber getSubscriber()
    {
        return subscriber;
    }
}

class BasicEventBusImpl implements BasicBus
{
    private static BasicEventBusImpl instance=null;

    private final List<Subscription> subscriptions = new ArrayList<>();

    private BasicEventBusImpl() {}

    public static BasicEventBusImpl getInstance()
    {
        if(instance==null)
        {
            instance= new BasicEventBusImpl();
        }
        return instance;
    }

    @Override
    public void publish(Event event)
    {
        for(Subscription subscription : subscriptions)
        {
            if(subscription.getEventType().isAssignableFrom(event.getClass()))
            {
                subscription.getSubscriber().inform(event);
            }
        }
    }

    public void subscribe(Class<?> eventType, Subscriber subscriber)
    {
        subscriptions.add(new Subscription(eventType, subscriber));
    }

    public void unsubscribe(Class<?> eventType, Subscriber subscriber)
    {
        subscriptions.removeIf(subscription -> subscription.getEventType().equals(eventType) && subscription.getSubscriber().equals(subscriber));
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
        BasicEventBusImpl.getInstance().publish(new TemperatureEvent(this, temperature));
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
        BasicEventBusImpl.getInstance().publish(new WaterLevelEvent(this, waterLevel));
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
                BasicEventBusImpl.getInstance().publish(new SportsNewsEvent(this, newsContent));
                break;
            case "political": 
                BasicEventBusImpl.getInstance().publish(new PoliticalNewsEvent(this, newsContent));
                break;
            case "culture": 
                BasicEventBusImpl.getInstance().publish(new CultureNewsEvent(this, newsContent));
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

class NumericDisplay implements Subscriber
{
    private final String name;

    public NumericDisplay(String name)
    {
        this.name=name;
    }

    @Override
    public void inform(Event event)
    {
        if(event instanceof TemperatureEvent)
        {
            TemperatureEvent tempEvent = (TemperatureEvent) event;
            System.out.println(name + " - Temperature: " + tempEvent.getTemperature() + "°C");
        }
        if(event instanceof WaterLevelEvent)
        {
            WaterLevelEvent waterEvent=(WaterLevelEvent) event;
            System.out.println(name+" - Water Level: "+waterEvent.getWaterLevel());
        }
    }
}

class TextDisplay implements Subscriber
{
    private final String name;

    public TextDisplay(String name)
    {
        this.name=name;
    }

    @Override
    public void inform(Event event)
    {
        if(event instanceof TemperatureEvent)
        {
            TemperatureEvent tempEvent = (TemperatureEvent) event;
            String text = tempEvent.getTemperature() <20 ? "Cold " :"Warm ";
            System.out.println(name+" - "+text);
        }
        if(event instanceof WaterLevelEvent)
        {
            WaterLevelEvent waterEvent=(WaterLevelEvent) event;
            String text = waterEvent.getWaterLevel()<36 ?"All good ":"Run for your lives";
            System.out.println(name+" - "+text);
        }
    }
}

class HumanSubscriber implements Subscriber
{
    private final String name;

    public HumanSubscriber(String name)
    {
        this.name=name;
    }

    @Override
    public void inform(Event event)
    {
        if(event instanceof NewsEvent)
        {
            NewsEvent newsEvent = (NewsEvent) event;
            System.out.println(name + " received news from " + newsEvent.getAgency() + ": " + newsEvent.getContent());
        }
    }
}

public class BasicEventBus 
{
    public static void main(String[] args) 
    {
        BasicBus bus = BasicEventBusImpl.getInstance();

        NumericDisplay numericDisplay= new NumericDisplay("Numeric Display");
        TextDisplay textDisplay=new TextDisplay("Text Display");

        bus.subscribe(TemperatureEvent.class, numericDisplay);
        bus.subscribe(TemperatureEvent.class, textDisplay);
        bus.subscribe(WaterLevelEvent.class, numericDisplay);
        bus.subscribe(WaterLevelEvent.class, textDisplay);

        TemperatureSensor temperatureSensor1=new TemperatureSensor("tS1");
        TemperatureSensor temperatureSensor2=new TemperatureSensor("tS2");
        WaterLevelSensor waterLevelSensor1=new WaterLevelSensor("wS1");

        NewsAgency proTV=new NewsAgency("ProTV");
        NewsAgency digi24=new NewsAgency("Digi24");

        HumanSubscriber vasile =new HumanSubscriber("Vasile");
        HumanSubscriber ghita =new HumanSubscriber("Ghita");

        bus.subscribe(SportsNewsEvent.class, ghita);
        bus.subscribe(PoliticalNewsEvent.class, vasile);
        bus.subscribe(SportsNewsEvent.class, vasile);

        for(int i=0; i<5; i++)
        {
            temperatureSensor1.generateTemperature();
            temperatureSensor2.generateTemperature();
            waterLevelSensor1.generateWaterLevel();

            System.out.println();

            try
            {
                Thread.sleep(100);
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        System.out.println();

        for(int i=0; i<3; i++)
        {
            proTV.publishNews("political");
            digi24.publishNews("sports");
        }
    }    
}
