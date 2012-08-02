package com.aplana.iask.mus.test.persistence.dao;

import com.aplana.iask.mus.test.db.JDBCConnector;
import com.aplana.iask.mus.test.persistence.entity.GetOperationDataIn;
import com.aplana.iask.mus.test.persistence.entity.GetOperationDataOut;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author rshamsutdinov
 * @version 1.0
 */
public class MusOpDao {

    public static final String GET_OPERATION_DATA =
            "SET NOCOUNT ON;\n" +
            "declare @noperid int = ?\n" +
            "declare @login varchar (30) = ?\n" +
            "--\n" +
            "if OBJECT_ID('tempdb..#tLapMaster') is not null drop table #tLapMaster\n" +
            "create table #tLapMaster (nPackID int, sPackTitle varchar (30), sName varchar (50), nOperWhose int, nftypeid int)\n" +
            "if @login is null\n" +
            "begin\n" +
            "insert into #tLapMaster (nPackID, sPackTitle, sName, nOperWhose, nftypeid)\n" +
            "select p.nPackID, c.sPackTitle , tU.sName1, sd.NoperWhose, tU.nftypeid\n" +
            "from tPackList p\n" +
            "join tCreData c on p.nPackID = c.nPackID\n" +
            "join tClients l on l.nClientID = c.nClientID\n" +
            "join tScensData sd on p.nLapMaster = sd.nLapScID\n" +
            "join tPackRights tPR on tPR.nPackID = p.nPackID\n" +
            "join tUsers tU on tU.nGID = tPR.nCUID\n" +
            "where\n" +
            "sd.nOperID = @noperid\n" +
            "and sd.nActual = 1\n" +
            "and p.nDeleted = 0\n" +
            "and p.ntypepackid not in (5)\n" +
            "end\n" +
            "if @login is not null\n" +
            "begin\n" +
            "insert into #tLapMaster (nPackID, sPackTitle, sName, NoperWhose, nftypeid )\n" +
            "select p.nPackID, c.sPackTitle , tU.sName1, sd.NoperWhose, tU.nftypeid\n" +
            "from tPackList p\n" +
            "join tCreData c on p.nPackID = c.nPackID\n" +
            "join tClients l on l.nClientID = c.nClientID\n" +
            "join tScensData sd on p.nLapMaster = sd.nLapScID\n" +
            "join tPackRights tPR on tPR.nPackID = p.nPackID\n" +
            "join tUsers tU on tU.nGID = tPR.nCUID\n" +
            "where\n" +
            "sd.nOperID = @noperid\n" +
            "and sd.nActual = 1\n" +
            "and p.nDeleted = 0\n" +
            "and tU.sName1=@login\n" +
            "and p.ntypepackid not in (5)\n" +
            "end\n" +
            "--\n" +
            "if OBJECT_ID('tempdb..#tLapSlave') is not null drop table #tLapslave\n" +
            "create table #tLapslave (nPackID int, sPackTitle varchar (30), sName varchar (50), nOperWhose int, nftypeid int)\n" +
            "if @login is null\n" +
            "begin\n" +
            "insert into #tLapslave (nPackID, sPackTitle, sName, NoperWhose, nftypeid)\n" +
            "select p.nPackID, c.sPackTitle , tU.sName1, sd.NoperWhose, tU.nftypeid\n" +
            "from tPackList p\n" +
            "join tCreData c on p.nPackID = c.nPackID\n" +
            "join tClients l on l.nClientID = c.nClientID\n" +
            "join tScensData sd on p.nLapslave = sd.nLapScID\n" +
            "join tPackRights tPR on tPR.nPackID = p.nPackID\n" +
            "join tUsers tU on tU.nGID = tPR.nCUID\n" +
            "where\n" +
            "sd.nOperID = @noperid\n" +
            "and sd.nActual = 1\n" +
            "and p.nDeleted = 0\n" +
            "and p.ntypepackid not in (5)\n" +
            "end\n" +
            "if @login is not null\n" +
            "begin\n" +
            "insert into #tLapslave (nPackID, sPackTitle, sName, nOperWhose, nftypeid)\n" +
            "select p.nPackID, c.sPackTitle , tU.sName1,sd.NoperWhose, tU.nftypeid\n" +
            "from tPackList p\n" +
            "join tCreData c on p.nPackID = c.nPackID\n" +
            "join tClients l on l.nClientID = c.nClientID\n" +
            "join tScensData sd on p.nLapslave = sd.nLapScID\n" +
            "join tPackRights tPR on tPR.nPackID = p.nPackID\n" +
            "join tUsers tU on tU.nGID = tPR.nCUID\n" +
            "where\n" +
            "sd.nOperID = @noperid\n" +
            "and sd.nActual = 1\n" +
            "and p.nDeleted = 0\n" +
            "and tU.sName1=@login\n" +
            "and p.ntypepackid not in (5)\n" +
            "end\n" +
            " \n" +
            "--\n" +
            "if OBJECT_ID('tempdb..#tAll') is not null drop table #tAll\n" +
            "create table #tAll (nPackID int, sPackTitle varchar (30), sName varchar (50), nOperWhose int, nftypeid int, nExec varchar(20), usertype int)\n" +
            "insert into #tAll (nPackID, sPackTitle, sName, nOperWhose, nftypeid)\n" +
            "select nPackID , sPackTitle , sName, nOperWhose,nftypeid\n" +
            "from #tLapMaster\n" +
            "insert into #tAll (nPackID, sPackTitle, sName, nOperWhose, nftypeid)\n" +
            "select nPackID , sPackTitle , sName,nOperWhose, nftypeid \n" +
            "from #tlapslave\n" +
            "if @login is null\n" +
            "begin\n" +
            "update #tAll\n" +
            "set usertype=2\n" +
            "where nFTypeid in (5,6,7)\n" +
            "update #tAll\n" +
            "set usertype=4\n" +
            "where nFTypeid = 4\n" +
            "update #tAll\n" +
            "set usertype=8\n" +
            "where nFTypeid in (17,18)\n" +
            "update #tAll\n" +
            "set usertype=16\n" +
            "where nFTypeid in (19,20)\n" +
            "update #tAll\n" +
            "set usertype=32\n" +
            "where nFTypeid in (21)\n" +
            "update #tAll\n" +
            "set usertype=64\n" +
            "where nFTypeid in (12)\n" +
            "update #tAll\n" +
            "set usertype=1\n" +
            "where nFTypeid in (11)\n" +
            "update #tAll\n" +
            "set usertype=128\n" +
            "where nFTypeid in (37,38)\n" +
            "delete from #tall\n" +
            "where (noperwhose <> usertype and noperwhose in (2,4,8,16,32)) or (noperwhose in (2,4,8,16,32) and usertype is null) or (noperwhose=24 and usertype in (2,4,32))\n" +
            "or (noperwhose=9 and usertype in (2,4,16,32)) or (noperwhose=10 and usertype in (1,4,16,32)) or (noperwhose=18 and usertype in (1,4,8,32))\n" +
            "or (noperwhose=34 and usertype in (1,4,8,16))or (nftypeid not in (4,5,6,17,18,19,20,21)) or noperwhose is null\n" +
            "end\n" +
            "select top 1 tot.sOperName as 'opName', #tAll.sPackTitle as 'packTitle'\n" +
            "from #tAll\n" +
            "join tOperTypes tot on tot.nOperID = @noperid\n" +
            "where #tAll.sPackTitle is not null and LEN(#tAll.sPackTitle) > 3\n" +
            "order by sName;";

    private Connection connection;

    public MusOpDao() {
        connection = new JDBCConnector().open();
    }

    public GetOperationDataOut getOperationData(GetOperationDataIn in) throws MusOpDaoException {
        final GetOperationDataOut out = new GetOperationDataOut();

        try {
            final PreparedStatement getOperationData = connection.prepareStatement(GET_OPERATION_DATA);
            getOperationData.setInt(1, in.getOpNum());
            getOperationData.setString(2, in.getLogin());

            getOperationData.execute();

            final ResultSet res = getOperationData.getResultSet();
            res.next();

            out.setOpName(res.getString("opName"));
            out.setPackTitle(res.getString("packTitle"));
        } catch (SQLException e) {
            throw new MusOpDaoException(String.format("Не удалось получить данные по операции %d", in.getOpNum()), e);
        }

        return out;
    }

}
