package com.aplana.iask.mus.test;

import com.aplana.iask.mus.test.persistence.dao.MusOpDao;
import com.aplana.iask.mus.test.persistence.entity.GetOperationDataIn;
import com.aplana.iask.mus.test.persistence.entity.GetOperationDataOut;
import com.aplana.iask.mus.test.selenium.IaskSelenium;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import static com.aplana.iask.mus.test.Settings.IASK_URL;
import static com.aplana.iask.mus.test.Settings.get;

/**
 * @author rshamsutdinov
 * @version 1.0
 */
public class MusTest {

    public static final String OPERATION_PROPS = "operations.properties";

    private static final Map<String, List<Integer>> operationsMap = new HashMap<String, List<Integer>>();

    static {
//        addOperation(13,	"23ЮИ"); // TODO: unique
    }

    private static void addOperation(int opNum, String login) {
        List<Integer> operationsList = operationsMap.get(login);
        if (operationsList == null) {
            operationsList = new ArrayList<Integer>();
            operationsMap.put(login, operationsList);
        }

        operationsList.add(opNum);
    }

    private static final PrintStream OUT = System.out;
    private static MusOpDao musOpDao;

    public static void main(String[] args) throws IOException {
        loadOperations();

        musOpDao = new MusOpDao();

        IaskSelenium selenium = null;

        try {
            selenium = new IaskSelenium(get(IASK_URL));
            selenium.getDriver().manage().window().maximize();

            for (String login : operationsMap.keySet()) {
                if (selenium.isAuthorized()) {
                    selenium.deauthorize();
                }

                selenium.waitLoading(1);
                selenium.authorization(login);
                selenium.waitLoading(2);

                for (Integer operation : operationsMap.get(login)) {
                    selenium.closeAnyBox();

                    GetOperationDataIn getOperationDataIn = new GetOperationDataIn();
                    getOperationDataIn.setOpNum(operation);
                    getOperationDataIn.setLogin(login);

                    GetOperationDataOut getOperationDataOut = musOpDao.getOperationData(getOperationDataIn);

                    try {
                        openAgreement(selenium, getOperationDataOut);

                        OUT.println(String.format("Операция %d - OK", operation));
                    } catch (Exception e) {
                        OUT.println(String.format("Ошибка выполнения операции %d (%s): %s\n* Пользователь: %s\n* Договор: %s",
                                operation, getOperationDataOut.getOpName(), e.getMessage(), login,
                                getOperationDataOut.getPackTitle()));
                    }
                }
            }
        } catch (Throwable th) {
            OUT.println(th.toString());
        } finally {
            if (selenium != null) {
                selenium.close();
            }
        }
    }

    private static void loadOperations() throws IOException {
        Properties properties = new Properties();

        FileInputStream fileInputStream = new FileInputStream(OPERATION_PROPS);
        properties.load(fileInputStream);
        fileInputStream.close();

        for (String propertyName : properties.stringPropertyNames()) {
            addOperation(Integer.valueOf(propertyName), properties.getProperty(propertyName));
        }
    }

    private static void openAgreement(IaskSelenium selenium, GetOperationDataOut getOperationDataOut) throws Exception {
        final RemoteWebDriver driver = selenium.getDriver();
        WebElement element;

        findAgreement(selenium, driver, getOperationDataOut);

        Thread.sleep(50);

        editInMus(selenium, driver);

        boolean tabNotSwitched = true;
        try {
            while (true) {
                List<WebElement> elements = driver.findElementsByXPath
                        (
                                "//div[@class = 'x-panel-bwrap']//table[@class = 'x-grid3-row-table']" +
                                        "//tr[@role = 'presentation']//button/ancestor::tr//div[@class = 'x-grid3-cell-inner x-grid3-col-name']"
                        );

                final int size = elements.size();

                if (size == 0) {
                    checkForErrorBox(driver);
                    throw new Exception("Нет операций, доступных для выполнения.");
                }

                for (int i = 0; i < size; i++) {
                    element = elements.get(i);
                    if (element.getText().equals(getOperationDataOut.getOpName())) {
                        new Actions(driver).doubleClick(element).build().perform();

                        selenium.waitLoading(3);

                        checkForErrorBox(driver);

                        return;
                    }
                }
                if (tabNotSwitched) {
                    tabNotSwitched = false;
                    element = driver.findElement(By.xpath("//li[not(contains(@class, 'x-tab-strip-active'))]//span[starts-with(text(), " +
                            "'<<')]/ancestor::li"));
                    element.click();
                    selenium.waitLoading(2);
                } else {
                    break;
                }
            }
        } finally {
            driver.switchTo().defaultContent();
        }
        throw new Exception("Операция недоступна для выполнения.");
    }

    private static void checkForErrorBox(RemoteWebDriver driver) throws Exception {
        WebElement element = null;
        try {
            element = driver.findElement(By.xpath("//span[text() = 'Предупреждение " +
                    "системы']/ancestor::div[normalize-space(@class) = 'x-window x-component']/div[@class = 'x-window-bwrap']//div[@class = 'gwt-Label x-component']"));

        } catch (NoSuchElementException nsee) {
            try {
                element = driver.findElement(By.xpath("//span[text() = 'Ошибка']/ancestor::div[" +
                        "normalize-space(@class) = 'x-window x-component']/div[@class = 'x-window-bwrap']//div[@class = 'gwt-HTML' and position() > 1]"));
            } catch (NoSuchElementException e) {
            }
        }

        if (element != null) {
            throw new Exception(element.getText());
        }
    }

    private static void editInMus(IaskSelenium selenium, RemoteWebDriver driver) throws InterruptedException {
        WebElement element = driver.findElementByXPath("//button[text() = 'Редактировать в МУС']");
        selenium.alternativeClick(element);

        selenium.waitLoading(4);

        driver.switchTo().frame(driver.findElementByXPath("//iframe[@class = 'gwt-Frame x-component']"));

        selenium.waitLoading(1);

        selenium.closeAnyBox();
    }

    private static void findAgreement(IaskSelenium selenium, RemoteWebDriver driver, GetOperationDataOut getOperationDataOut)
            throws Exception {
        selenium.workWithMenu(new String[]{"Договор", "Открыть договор"});
        selenium.waitLoading(2);

        WebElement element = driver.findElementByXPath("//div[text() = 'Номер договора']/ancestor::tr//input[1]");
        element.clear();
        element.sendKeys(getOperationDataOut.getPackTitle());

        element = driver.findElementByXPath("//button[text() = 'Применить']");
        selenium.alternativeClick(element);
        selenium.waitLoading(4);

        selenium.closeAnyBox();

        try {
            element = driver.findElementByXPath("//div[@class = 'x-grid3-cell-inner x-grid3-col-sPackTitle' and text() = '" + getOperationDataOut.getPackTitle() + "']");
            element.click();
        } catch (NoSuchElementException e) {
            throw new Exception(String.format("Договор %s не найден.", getOperationDataOut.getPackTitle()));
        }
    }
}
