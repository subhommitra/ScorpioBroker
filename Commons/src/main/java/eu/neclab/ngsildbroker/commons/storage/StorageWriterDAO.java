package eu.neclab.ngsildbroker.commons.storage;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import eu.neclab.ngsildbroker.commons.constants.AppConstants;
import eu.neclab.ngsildbroker.commons.constants.DBConstants;
import eu.neclab.ngsildbroker.commons.constants.NGSIConstants;
import eu.neclab.ngsildbroker.commons.datatypes.BaseRequest;
import eu.neclab.ngsildbroker.commons.datatypes.EntityRequest;
import eu.neclab.ngsildbroker.commons.datatypes.HistoryEntityRequest;
import eu.neclab.ngsildbroker.commons.datatypes.TemporalEntityStorageKey;
import eu.neclab.ngsildbroker.commons.serialization.DataSerializer;
import eu.neclab.ngsildbroker.commons.tenant.TenantAwareDataSource;
import eu.neclab.ngsildbroker.commons.tenant.TenantContext;

@Repository
@ConditionalOnProperty(value = "writer.enabled", havingValue = "true", matchIfMissing = false)
public class StorageWriterDAO {

	private final static Logger logger = LogManager.getLogger(StorageWriterDAO.class);
//	public static final Gson GSON = DataSerializer.GSON;

	@Autowired
	private JdbcTemplate writerJdbcTemplate;
	@Autowired
	TenantAwareDataSource tenantAwareDataSource;

	@Autowired
	private DataSource writerDataSource;

	private TransactionTemplate writerTransactionTemplate;
	private JdbcTemplate writerJdbcTemplateWithTransaction;

	@PostConstruct
	public void init() {
		writerJdbcTemplate.execute("SELECT 1"); // create connection pool and connect to database

		// https://gist.github.com/mdellabitta/1444003
		DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(writerDataSource);
		writerJdbcTemplateWithTransaction = new JdbcTemplate(transactionManager.getDataSource());
		writerTransactionTemplate = new TransactionTemplate(transactionManager);
	}

	public boolean storeTenantdata(String tableName, String columnName, String tenantidvalue, String databasename)
			throws SQLException {
		writerJdbcTemplate = new JdbcTemplate(writerDataSource);

		try {
			String sql;
			int n = 0;
			if (!tenantidvalue.equals(null)) {
				sql = "INSERT INTO " + tableName
						+ " (tenant_id, database_name) VALUES (?, ?) ON CONFLICT(tenant_id) DO UPDATE SET tenant_id = EXCLUDED.tenant_id";
				n = writerJdbcTemplate.update(sql, tenantidvalue, databasename);
			} else {
				sql = "DELETE FROM " + tableName + " WHERE id = ?";
				n = writerJdbcTemplate.update(sql, tenantidvalue);
			}
			logger.trace("Rows affected: " + Integer.toString(n));
			return true; // (n>0);
		} catch (Exception e) {
			logger.error("Exception ::", e);
			e.printStackTrace();
		}
		return false;
	}

	public String findDataBaseNameByTenantId(String tenantidvalue) {
		if (tenantidvalue == null)
			return null;
		try {

			// SELECT EXISTS(SELECT datname FROM pg_database WHERE datname = 'tenant2');
			String databasename = writerJdbcTemplate.queryForObject(
					"SELECT database_name FROM tenant WHERE tenant_id = ?", String.class, tenantidvalue);
			List<String> data = writerJdbcTemplate.queryForList("SELECT datname FROM pg_database", String.class);
			if (data.contains(databasename)) {
				return databasename;
			} else {
				String sql = "create database " + databasename + "";
				writerJdbcTemplate.execute(sql);
				return databasename;
			}
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public boolean store(String tableName, String columnName, String key, String value, String tenantvalue)
			throws SQLException {
		try {
			String sql;
			int n = 0;
			if (tenantvalue != null) {
				DataSource finaldatasource = tenantAwareDataSource.determineTargetDataSource();
				writerJdbcTemplate = new JdbcTemplate(finaldatasource);
			} else {
				writerJdbcTemplate = new JdbcTemplate(writerDataSource);
			}
			if (!value.equals("null")) {
				sql = "INSERT INTO " + tableName + " (id, " + columnName
						+ ") VALUES (?, ?::jsonb) ON CONFLICT(id) DO UPDATE SET " + columnName + " = EXCLUDED."
						+ columnName;
				n = writerJdbcTemplate.update(sql, key, value);
			} else {
				sql = "DELETE FROM " + tableName + " WHERE id = ?";
				n = writerJdbcTemplate.update(sql, key);
			}

			logger.trace("Rows affected: " + Integer.toString(n));
			return true; // (n>0);
		} catch (Exception e) {
			logger.error("Exception ::", e);
			e.printStackTrace();
		}

		return false;
	}

	public boolean storeTemporalEntity(HistoryEntityRequest request) throws SQLException {
		boolean result = true;
		setJDBCTemplate(request);
		// TemporalEntityStorageKey tesk =
		// DataSerializer.getTemporalEntityStorageKey(key);

		String entityId = request.getId();
		String entityType = request.getType();
		String entityCreatedAt = request.getCreatedAt();
		String entityModifiedAt = request.getModifiedAt();
		String instanceId = request.getInstanceId();
		String value;
		Integer attributeCount = 0;
		for (Map.Entry<String, JsonElement> entry : request.getJsonObject().entrySet()) {
			String attribId = entry.getKey();
			logger.debug("Key = " + attribId + " Value = " + entry.getValue());
			if (attribId.equalsIgnoreCase(NGSIConstants.JSON_LD_ID)
					|| attribId.equalsIgnoreCase(NGSIConstants.JSON_LD_TYPE)
					|| attribId.equalsIgnoreCase(NGSIConstants.NGSI_LD_CREATED_AT)
					|| attribId.equalsIgnoreCase(NGSIConstants.NGSI_LD_MODIFIED_AT)) {
				continue;
			}

			if (entry.getValue().isJsonArray()) {
				JsonArray valueArray = entry.getValue().getAsJsonArray();
				Integer instanceCount = 0;
				for (JsonElement jsonElement : valueArray) {

					//
					Boolean overwriteOp = (instanceCount == 0); // if it's the first one, send the overwrite op to
																// delete current values
					// Boolean createTemporalEntityIfNotExists = (attributeCount == 0);
					value = jsonElement.toString();
					/*
					 * pushAttributeToKafka(String entityId, String entityType, String
					 * entityCreatedAt, String entityModifiedAt, String attributeId, String
					 * elementValue, Boolean createTemporalEntityIfNotExists, Boolean overwriteOp)
					 */
					// pushAttributeToKafka(entityId, null, null, now, attribId,
					// jsonElement.toString(), false,
					// overwriteOp);
					result = result && doTemporalSqlAttrInsert(value, entityId, entityType, attribId, entityCreatedAt,
							entityModifiedAt, instanceId, overwriteOp);
					instanceCount++;
				}
			}
			attributeCount++;
		}
		return result;
	}

	private boolean doTemporalSqlAttrInsert(String value, String entityId, String entityType, String attributeId,
			String entityCreatedAt, String entityModifiedAt, String instanceId, Boolean overwriteOp) {
		try {
			Integer n = 0;

			if (!value.equals("null")) {
				// https://gist.github.com/mdellabitta/1444003
				n = writerTransactionTemplate.execute(new TransactionCallback<Integer>() {
					@Override
					public Integer doInTransaction(TransactionStatus status) {
						String sql;
						Integer tn = 0;
						if (entityId != null && entityType != null && entityCreatedAt != null
								&& entityModifiedAt != null) {
							sql = "INSERT INTO " + DBConstants.DBTABLE_TEMPORALENTITY
									+ " (id, type, createdat, modifiedat) VALUES (?, ?, ?::timestamp, ?::timestamp) ON CONFLICT(id) DO UPDATE SET type = EXCLUDED.type, createdat = EXCLUDED.createdat, modifiedat = EXCLUDED.modifiedat";
							synchronized (writerJdbcTemplate) {
								tn = writerJdbcTemplateWithTransaction.update(sql, entityId, entityType,
										entityCreatedAt, entityModifiedAt);
							}
						}

						if (entityId != null && attributeId != null) {
							if (overwriteOp != null && overwriteOp) {
								sql = "DELETE FROM " + DBConstants.DBTABLE_TEMPORALENTITY_ATTRIBUTEINSTANCE
										+ " WHERE temporalentity_id = ? AND attributeid = ?";
								tn += writerJdbcTemplateWithTransaction.update(sql, entityId, attributeId);
							}
							sql = "INSERT INTO " + DBConstants.DBTABLE_TEMPORALENTITY_ATTRIBUTEINSTANCE
									+ " (temporalentity_id, attributeid, data) VALUES (?, ?, ?::jsonb) ON CONFLICT(temporalentity_id, attributeid, instanceid) DO UPDATE SET data = EXCLUDED.data";
							tn += writerJdbcTemplateWithTransaction.update(sql, entityId, attributeId, value);
							// update modifiedat field in temporalentity
							sql = "UPDATE " + DBConstants.DBTABLE_TEMPORALENTITY
									+ " SET modifiedat = ?::timestamp WHERE id = ?";
							synchronized (writerJdbcTemplate) {
								tn += writerJdbcTemplateWithTransaction.update(sql, entityModifiedAt, entityId);
							}
						}
						return tn;

					}
				});
			} else {
				String sql;
				if (entityId != null && attributeId != null && instanceId != null) {
					sql = "DELETE FROM " + DBConstants.DBTABLE_TEMPORALENTITY_ATTRIBUTEINSTANCE
							+ " WHERE temporalentity_id = ? AND attributeid = ? AND instanceid = ?";
					synchronized (writerJdbcTemplate) {
						n = writerJdbcTemplate.update(sql, entityId, attributeId, instanceId);
					}
				} else if (entityId != null && attributeId != null) {
					sql = "DELETE FROM " + DBConstants.DBTABLE_TEMPORALENTITY_ATTRIBUTEINSTANCE
							+ " WHERE temporalentity_id = ? AND attributeid = ?";
					synchronized (writerJdbcTemplate) {
						n = writerJdbcTemplate.update(sql, entityId, attributeId);
					}
				} else if (entityId != null) {
					sql = "DELETE FROM " + DBConstants.DBTABLE_TEMPORALENTITY + " WHERE id = ?";
					synchronized (writerJdbcTemplate) {
						n = writerJdbcTemplate.update(sql, entityId);
					}
				}
			}

			logger.debug("Rows affected: " + Integer.toString(n));
			return true;
		} catch (Exception e) {
			logger.error("Exception ::", e);
			e.printStackTrace();
		}
		return false;

	}

	public boolean OldstoreTemporalEntity(String key, String value) throws SQLException {
		try {

			TemporalEntityStorageKey tesk = DataSerializer.getTemporalEntityStorageKey(key);

			String entityId = tesk.getEntityId();
			String entityType = tesk.getEntityType();
			String entityCreatedAt = tesk.getEntityCreatedAt();
			String entityModifiedAt = tesk.getEntityModifiedAt();

			String attributeId = tesk.getAttributeId();
			String instanceId = tesk.getInstanceId();
			Boolean overwriteOp = tesk.getOverwriteOp();

			Integer n = 0;

			if (!value.equals("null")) {
				// https://gist.github.com/mdellabitta/1444003
				n = writerTransactionTemplate.execute(new TransactionCallback<Integer>() {
					@Override
					public Integer doInTransaction(TransactionStatus status) {
						String sql;
						Integer tn = 0;
						if (entityId != null && entityType != null && entityCreatedAt != null
								&& entityModifiedAt != null) {
							sql = "INSERT INTO " + DBConstants.DBTABLE_TEMPORALENTITY
									+ " (id, type, createdat, modifiedat) VALUES (?, ?, ?::timestamp, ?::timestamp) ON CONFLICT(id) DO UPDATE SET type = EXCLUDED.type, createdat = EXCLUDED.createdat, modifiedat = EXCLUDED.modifiedat";
							tn = writerJdbcTemplateWithTransaction.update(sql, entityId, entityType, entityCreatedAt,
									entityModifiedAt);
						}

						if (entityId != null && attributeId != null) {
							if (overwriteOp != null && overwriteOp) {
								sql = "DELETE FROM " + DBConstants.DBTABLE_TEMPORALENTITY_ATTRIBUTEINSTANCE
										+ " WHERE temporalentity_id = ? AND attributeid = ?";
								tn += writerJdbcTemplateWithTransaction.update(sql, entityId, attributeId);
							}
							sql = "INSERT INTO " + DBConstants.DBTABLE_TEMPORALENTITY_ATTRIBUTEINSTANCE
									+ " (temporalentity_id, attributeid, data) VALUES (?, ?, ?::jsonb) ON CONFLICT(temporalentity_id, attributeid, instanceid) DO UPDATE SET data = EXCLUDED.data";
							tn += writerJdbcTemplateWithTransaction.update(sql, entityId, attributeId, value);
							// update modifiedat field in temporalentity
							sql = "UPDATE " + DBConstants.DBTABLE_TEMPORALENTITY
									+ " SET modifiedat = ?::timestamp WHERE id = ?";
							tn += writerJdbcTemplateWithTransaction.update(sql, entityModifiedAt, entityId);
						}
						return tn;

					}
				});
			} else {
				String sql;
				if (entityId != null && attributeId != null && instanceId != null) {
					sql = "DELETE FROM " + DBConstants.DBTABLE_TEMPORALENTITY_ATTRIBUTEINSTANCE
							+ " WHERE temporalentity_id = ? AND attributeid = ? AND instanceid = ?";
					n = writerJdbcTemplate.update(sql, entityId, attributeId, instanceId);
				} else if (entityId != null && attributeId != null) {
					sql = "DELETE FROM " + DBConstants.DBTABLE_TEMPORALENTITY_ATTRIBUTEINSTANCE
							+ " WHERE temporalentity_id = ? AND attributeid = ?";
					n = writerJdbcTemplate.update(sql, entityId, attributeId);
				} else if (entityId != null) {
					sql = "DELETE FROM " + DBConstants.DBTABLE_TEMPORALENTITY + " WHERE id = ?";
					n = writerJdbcTemplate.update(sql, entityId);
				}
			}

			logger.debug("Rows affected: " + Integer.toString(n));
			return true;
		} catch (Exception e) {
			logger.error("Exception ::", e);
			e.printStackTrace();
		}
		return false;
	}

	public boolean storeEntity(EntityRequest request) throws SQLTransientConnectionException {

		String sql;
		String key = request.getId();
		String value = request.getWithSysAttrs();
		String valueWithoutSysAttrs = request.getEntityWithoutSysAttrs();
		String kvValue = request.getKeyValue();
		int n = 0;

		setJDBCTemplate(request);
		if (value != null && !value.equals("null")) {
			sql = "INSERT INTO " + DBConstants.DBTABLE_ENTITY + " (id, " + DBConstants.DBCOLUMN_DATA + ", "
					+ DBConstants.DBCOLUMN_DATA_WITHOUT_SYSATTRS + ",  " + DBConstants.DBCOLUMN_KVDATA
					+ ") VALUES (?, ?::jsonb, ?::jsonb, ?::jsonb) ON CONFLICT(id) DO UPDATE SET ("
					+ DBConstants.DBCOLUMN_DATA + ", " + DBConstants.DBCOLUMN_DATA_WITHOUT_SYSATTRS + ",  "
					+ DBConstants.DBCOLUMN_KVDATA + ") = (EXCLUDED." + DBConstants.DBCOLUMN_DATA + ", EXCLUDED."
					+ DBConstants.DBCOLUMN_DATA_WITHOUT_SYSATTRS + ",  EXCLUDED." + DBConstants.DBCOLUMN_KVDATA + ")";
			synchronized (writerJdbcTemplate) {
				n = writerJdbcTemplate.update(sql, key, value, valueWithoutSysAttrs, kvValue);
			}
		} else {
			sql = "DELETE FROM " + DBConstants.DBTABLE_ENTITY + " WHERE id = ?";
			synchronized (writerJdbcTemplate) {
				n = writerJdbcTemplate.update(sql, key);
			}
		}
		logger.trace("Rows affected: " + Integer.toString(n));
		return true; // (n>0);

	}

	private void setJDBCTemplate(BaseRequest request) {
		synchronized (writerJdbcTemplate) {
			String tenant = getTenant(request);
			DataSource finalDataSource;
			if (tenant != null) {
				finalDataSource = tenantAwareDataSource.determineTargetDataSource();
			} else {
				finalDataSource = writerDataSource;
			}
			DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(finalDataSource);
			writerJdbcTemplateWithTransaction = new JdbcTemplate(transactionManager.getDataSource());
			writerTransactionTemplate = new TransactionTemplate(transactionManager);
			writerJdbcTemplate = new JdbcTemplate(finalDataSource);
		}
	}

	private String getTenant(BaseRequest request) {
		String tenant;
		if (request.getHeaders().containsKey(AppConstants.TENANT_HEADER)) {
			tenant = request.getHeaders().get(AppConstants.TENANT_HEADER).get(0);

			TenantContext.setCurrentTenant(tenant);
			String databasename = "ngb" + tenant;

			try {
				tenantAwareDataSource.storeTenantdata(DBConstants.DBTABLE_CSOURCE_TENANT,
						DBConstants.DBCOLUMN_DATA_TENANT, tenant, databasename);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			tenant = null;
		}
		return tenant;

	}

}