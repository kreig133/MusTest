package com.aplana.iask.mus.test.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.aplana.iask.mus.test.Settings.DRIVER_URL;
import static com.aplana.iask.mus.test.Settings.get;

/**
 * @author rshamsutdinov
 * @version 1.0
 */
public class IaskSelenium {

    private static RemoteWebDriver getDefaultDriver() {
        DesiredCapabilities ieCapabilities = DesiredCapabilities.internetExplorer();
        ieCapabilities.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
        ieCapabilities.setJavascriptEnabled(true);

        try {
            return new RemoteWebDriver(new URL(get(DRIVER_URL)), ieCapabilities);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final RemoteWebDriver driver;

    public IaskSelenium(String url) {
        this(getDefaultDriver(), url);
    }

    public IaskSelenium(RemoteWebDriver driver, String url) {
        this(driver, 6000L, url);
    }

    public IaskSelenium(RemoteWebDriver driver, Long timeout, String url) {
        this.driver = driver;

        driver.manage().timeouts().pageLoadTimeout(timeout, TimeUnit.SECONDS);
        driver.manage().timeouts().setScriptTimeout(timeout, TimeUnit.SECONDS);

        driver.get(url);
    }

    public RemoteWebDriver getDriver() {
        return driver;
    }

    public void close() {
        try {
            driver.quit();
            driver.close();
        } catch (Exception e) {
        }
    }

    public void waitLoading(int secondsForWait) throws InterruptedException {
        int i = 0;
        while (true) {
            boolean find = false;
            try {
                driver.findElement(By.xpath("//span[text()='Подождите, Ваш запрос выполняется']"));
                find = true;
            } catch (Exception e) {
            }
            try {
                driver.findElement(By.xpath(
                        "//div[@display='block']/img[@src='resources/images/default/shared/large-loading.gif']"));
                find = true;
            } catch (Exception e) {
            }

            if (!find) {
                i++;
            } else {
                i = 0;
            }
            if (i == secondsForWait) {
                break;
            }
            Thread.sleep(1000L);
        }
    }

    public boolean isAuthorized() {
        return (driver.findElementsByXPath("//span[text() = 'ИАС \"Кредитование\" - добро пожаловать']").size() == 0);
    }

    public void authorization(String login) throws InterruptedException, AuthorizationException {
        if (!isAuthorized()) {
            WebElement element = driver.findElement(By.xpath("//input[1]"));

            element.clear();
            element.sendKeys(login);

            element = driver.findElement(By.className("gwt-PasswordTextBox"));

            element.clear();
            element.sendKeys("123456");

            element = driver.findElement(By.xpath("//button[1]"));
            alternativeClick(element);

            waitLoading(2);

            try {
                element = driver.findElementByXPath("//span[starts-with(text(), 'Ошибка')]/ancestor::div[normalize-space(@class) = 'x-window x-component']//div[@class = 'x-window-body']//td[@width = '100%']//div[@class = 'gwt-HTML']");
                throw new AuthorizationException(element.getText());
            } catch (NoSuchElementException e) {
            }

            try {
                element = driver.findElementByXPath("//span[starts-with(text(), " +
                        "'Критическая ошибка')]/ancestor::div[normalize-space(@class) = 'x-window x-component']//div[@class = " +
                        "'x-window-body']//td[@width = '100%']//div[@class = 'gwt-HTML']");
                throw new AuthorizationException(element.getText());
            } catch (NoSuchElementException e) {
            }

            try {
                driver.findElement(By.xpath("//div[starts-with(text(), 'Под Вашими учетными данными')]"));
                element = driver.findElement(By.xpath("//button[text()='Да']"));
                alternativeClick(element);
            } catch (Exception e) {
            }

            waitLoading(2);
        }
    }

    public void deauthorize() throws InterruptedException {
        if (isAuthorized()) {
            // "выйти" работает пока что как-то криво
//            try {
//                alternativeClick(driver.findElement(By.xpath("//a[text() = 'выйти']")));
//            } catch (NoSuchElementException e) {
//            }
            // старый метод деавторизации
            driver.manage().deleteCookieNamed("JSESSIONID");
            driver.navigate().refresh();
            waitLoading(1);
        }
    }

    public void workWithMenu(String[] path) throws InterruptedException {
        final String xpathExpression = "//td[@class='gwt-MenuItem']/span[text()='%s']";
        for (String s : path) {
            WebElement element = driver.findElement(By.xpath(String.format(xpathExpression, s)));
            (new Actions(driver)).moveToElement(element).build().perform();
            if (s.equals(path[path.length - 1])) {
                //element.click();
                alternativeClick(element);
            }
            Thread.sleep(50);
        }
    }

    public void alternativeClick(WebElement element) {
        new Actions(driver).clickAndHold(element).build().perform();
        if (element.getTagName().equals("button")) {
            element.sendKeys("\n");
        } else {
            element.click();
        }
    }

    public void closeAnyBox() {
        while (true) {
            try{
                List<WebElement> webElements = driver.findElements(By.xpath("//div[normalize-space(@class) = " +
                        "'x-nodrag x-tool-close x-tool x-component']"));
                if (webElements.isEmpty()) {
                    break;
                }

                for (int i = webElements.size() - 1; i >= 0; i--) {
                    alternativeClick(webElements.get(i));
                    Thread.sleep(50);
                }
            } catch (Exception e) {
                break;
            }
        }
    }

}
