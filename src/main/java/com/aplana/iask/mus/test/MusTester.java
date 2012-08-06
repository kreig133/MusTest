package com.aplana.iask.mus.test;

import com.aplana.iask.mus.test.persistence.dao.MusOpDao;
import com.aplana.iask.mus.test.persistence.dao.MusOpDaoException;
import com.aplana.iask.mus.test.persistence.entity.GetOperationDataIn;
import com.aplana.iask.mus.test.persistence.entity.GetOperationDataOut;
import com.aplana.iask.mus.test.selenium.AuthorizationException;
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

public class MusTester {

    public static final String  OPERATION_PROPS = "operations.properties";
    public static final int     AUTH_ATTEMPTS   = 3;

    private static final PrintStream OUT = System.out;

//    static {
//        addOperation(13,	"23ЮИ"); // TODO: unique
//    }

    private final Map<String, List<Integer>> operationsMap;
    private final MusOpDao musOpDao;
    private IaskSelenium iaskSelenium;
    private RemoteWebDriver webDriver;

    public MusTester() {
        operationsMap = new HashMap<String, List<Integer>>();
        musOpDao = new MusOpDao();
    }

    private void addOperation(int opNum, String login) {
        List<Integer> operationsList = operationsMap.get(login);
        if (operationsList == null) {
            operationsList = new ArrayList<Integer>();
            operationsMap.put(login, operationsList);
        }

        operationsList.add(opNum);
    }

    public void test() throws IOException {
        loadOperations();

        try {
            iaskSelenium = new IaskSelenium(Settings.get(Settings.IASK_URL));
            webDriver = iaskSelenium.getDriver();

            webDriver.manage().window().maximize();

            for (String login : operationsMap.keySet()) {
                if (iaskSelenium.isAuthorized()) {
                    iaskSelenium.deauthorize();
                }

                int authAttempts = AUTH_ATTEMPTS;
                while (true) {
                    try {
                        iaskSelenium.closeAnyBox();
                        iaskSelenium.waitLoading(1);
                        iaskSelenium.authorization(login);
                        iaskSelenium.waitLoading(2);
                        break;
                    } catch (AuthorizationException e) {
                        if (authAttempts-- == 0) {
                            throw e;
                        }
                        Thread.sleep(30 * 1000); // wait 30 seconds
                    }
                }

                for (Integer operation : operationsMap.get(login)) {
                    iaskSelenium.closeAnyBox();
                    iaskSelenium.closeAllTabs();

                    GetOperationDataIn getOperationDataIn = new GetOperationDataIn();
                    getOperationDataIn.setOpNum(operation);
                    getOperationDataIn.setLogin(login);

                    GetOperationDataOut getOperationDataOut;
                    try {
                        getOperationDataOut = musOpDao.getOperationData(getOperationDataIn);
                    } catch (MusOpDaoException e) {
                        OUT.println(e.toString());
                        continue;
                    }

                    try {
                        openAgreement(getOperationDataOut);

                        OUT.println(String.format("Операция %d - OK", operation));
                    } catch (Exception e) {
                        OUT.println(String.format(
                                "Ошибка выполнения операции %d (%s): %s\n* Пользователь: %s\n* Договор: %s",
                                operation, getOperationDataOut.getOpName(), e.getMessage(), login,
                                getOperationDataOut.getPackTitle()));
                    }
                }
            }
        } catch (Throwable th) {
            OUT.println(th.toString());
        } finally {
            if (iaskSelenium != null) {
                iaskSelenium.close();
            }
        }
    }

    private void loadOperations() throws IOException {
        Properties properties = new Properties();

        FileInputStream fileInputStream = new FileInputStream(OPERATION_PROPS);
        properties.load(fileInputStream);
        fileInputStream.close();

        for (String propertyName : properties.stringPropertyNames()) {
            addOperation(Integer.valueOf(propertyName), properties.getProperty(propertyName));
        }
    }

    private void openAgreement(GetOperationDataOut getOperationDataOut) throws Exception {
        WebElement element;

        findAgreement(getOperationDataOut);

        Thread.sleep(50);

        editSelectedAgreementInMus();

        boolean tabNotSwitched = true;
        try {
            while (true) {
                List<WebElement> elements = webDriver.findElementsByXPath
                        (
                                "//div[@class = 'x-panel-bwrap']//table[@class = 'x-grid3-row-table']" +
                                        "//tr[@role = 'presentation']//button/ancestor::tr//div[@class = 'x-grid3-cell-inner x-grid3-col-name']"
                        );

                final int size = elements.size();

                if (size == 0) {
                    checkForErrorBox();
                    throw new Exception("Нет операций, доступных для выполнения.");
                }

                iaskSelenium.closeAnyBox();

                for (int i = 0; i < size; i++) {
                    element = elements.get(i);
                    if (element.getText().equals(getOperationDataOut.getOpName())) {
                        new Actions(webDriver).doubleClick(element).build().perform();

                        iaskSelenium.waitLoading(3);

                        checkForErrorBox();

                        return;
                    }
                }
                if (tabNotSwitched) {
                    tabNotSwitched = false;
                    element = webDriver.findElement(
                            By.xpath("//li[not(contains(@class, 'x-tab-strip-active'))]//span[starts-with(text(), " +
                                    "'<<')]/ancestor::li"));
                    iaskSelenium.alternativeClick(element);
                    iaskSelenium.waitLoading(2);
                } else {
                    break;
                }
            }
        } finally {
            webDriver.switchTo().defaultContent();
        }
        throw new Exception("Операция недоступна для выполнения.");
    }

    private void checkForErrorBox() throws Exception {
        WebElement element = null;
        final String xpathExpression = "//span[starts-with(text(), '%s')]/ancestor::div[normalize-space(@class) = " +
                "'x-window x-component']/div[@class = 'x-window-bwrap']%s";
        try {
            element = webDriver.findElement(By.xpath(String.format(xpathExpression, "Предупреждение системы",
                    "//div[@class = 'gwt-Label x-component']")));
        } catch (org.openqa.selenium.NoSuchElementException nsee) {
            List<WebElement> webElements = webDriver.findElements(By.xpath(String.format(xpathExpression, "Ошибка",
                        "//div[@class = 'gwt-HTML']")));

            for (WebElement el : webElements) {
                if (el.getText().trim().length() > 0) {
                    element = el;
                    break;
                }
            }
        }

        if (element != null) {
            throw new Exception(element.getText());
        }
    }

    private void editSelectedAgreementInMus() throws InterruptedException {
        WebElement element = webDriver.findElementByXPath("//button[text() = 'Редактировать в МУС']");
        iaskSelenium.alternativeClick(element);

        iaskSelenium.waitLoading(4);

        webDriver.switchTo().frame(webDriver.findElementByXPath("//iframe[@class = 'gwt-Frame x-component']"));

        iaskSelenium.waitLoading(1);
    }

    private void findAgreement(GetOperationDataOut getOperationDataOut)
            throws Exception {
        iaskSelenium.workWithMenu(new String[] {"Договор", "Открыть договор"});
        iaskSelenium.waitLoading(2);

        WebElement element = webDriver.findElementByXPath("//div[text() = 'Номер договора']/ancestor::tr//input[1]");
        element.clear();
        element.sendKeys(getOperationDataOut.getPackTitle());

        element = webDriver.findElementByXPath("//button[text() = 'Применить']");
        iaskSelenium.alternativeClick(element);
        iaskSelenium.waitLoading(4);

        checkForErrorBox();

        iaskSelenium.closeAnyBox();

        try {
            element = webDriver.findElementByXPath(
                    "//div[@class = 'x-grid3-cell-inner x-grid3-col-sPackTitle' and text() = '" +
                            getOperationDataOut.getPackTitle() + "']");
            iaskSelenium.alternativeClick(element);
        } catch (NoSuchElementException e) {
            throw new Exception(String.format("Договор %s не найден.", getOperationDataOut.getPackTitle()));
        }
    }
}