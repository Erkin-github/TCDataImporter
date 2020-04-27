package com.custom.rac.datamanagement.importer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.custom.rac.datamanagement.util.AbstractImporter;
import com.custom.rac.datamanagement.util.MyClassifyManager;
import com.custom.rac.datamanagement.util.MyDatasetUtil;
import com.custom.rac.datamanagement.util.MyStatusUtil;
import com.custom.rac.datamanagement.util.PropertyContainer;
import com.custom.rac.datamanagement.util.XMLResult;
import com.sfgk.sie.webservice.SFGKServiceProxy;
import com.teamcenter.rac.aifrcp.AIFUtility;
import com.teamcenter.rac.kernel.TCComponent;
import com.teamcenter.rac.kernel.TCComponentFolder;
import com.teamcenter.rac.kernel.TCComponentFolderType;
import com.teamcenter.rac.kernel.TCComponentItemType;
import com.teamcenter.rac.kernel.TCComponentUser;
import com.teamcenter.rac.kernel.TCComponentUserType;
import com.teamcenter.rac.kernel.TCException;
import com.teamcenter.rac.kernel.TCSession;
import com.teamcenter.rac.util.MessageBox;

public class SFGKDocumentImporter extends AbstractImporter {

	TCSession session = (TCSession) AIFUtility.getDefaultSession();
	MyClassifyManager cls_manger = new MyClassifyManager(session);
	TCComponentFolder folder = null;
	SFGKServiceProxy proxy = new SFGKServiceProxy();
	
	@Override
	public String getName() {
		return "文档导入程序";
	}
	public String getType(String node_id) {
		String prefix = node_id.substring(0, 4);
		String type = "";
		switch (prefix) {
		case "SF-A":
			type = "SF8_GFBZDocument";
			break;
		case "SF-B":
			type = "SF8_SJZLDocument";
			break;
		case "SF-C":
			type = "SF8_YBZLDocument";
			break;
		case "SF-D":
			type = "SF8_GYZLDocument";
			break;
		case "SF-E":
			type = "SF8_XLHDocument";
			break;
		case "SF-F":
			type = "SF8_HTXMDocument";
			break;
		case "SF-G":
			type = "SF8_YFXMDocument";
			break;
		case "SF-H":
			type = "SF8_TBXMDocument";
			break;
		case "SF-J":
			type = "SF8_SYZXDocument";
			break;
		case "SF-K":
			type = "SF8_JSGLDocument";
			break;
		case "SF-X":
			type = "SF8_XHDocument";
			break;
		default:
			break;
		}
		return type;
	}

	@Override
	public TCComponentItemType getItemType(int index) throws Exception {
		String ics_id = getValue(index, "图文档分类ID") + "";
		TCComponentItemType type = (TCComponentItemType) session.getTypeComponent(getType(ics_id));
		return type;
	}

	@Override
	public PropertyContainer getPropertyContainer(int index) {
		return PropertyContainer.itemRevision;
	}

	@Override
	public void onSingleStart(int index) throws Exception{
		
	}

	@Override
	public void onSingleFinish(int index, TCComponent tcc) throws Exception {
		if (folder != null) {
			folder.add("contents", tcc);
		}
	}	

	@Override
	public void onSingleError(int index, Exception e) {
		System.out.println("第" +index+ "行异常：" + e.getMessage());
	}

	@Override
	public void onStart() throws TCException {
		TCComponentFolderType folderType = (TCComponentFolderType) session.getTypeComponent("Folder");
		Date date = new Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		String name = "文档历史数据导入" + format.format(date);
		folder = folderType.create(name, "", "Folder");
		session.getUser().getHomeFolder().add("contents", folder);
	}

	@Override
	public void onFinish() {
		MessageBox.post("导入完成","提示", MessageBox.INFORMATION);
	}

	@Override
	public boolean ignoreProperty(int index, String propertyDisplayName) {
		if (propertyDisplayName.equals("文档编号") || propertyDisplayName.equals("版本") 
			||propertyDisplayName.equals("文档名称")){
			return true;
		}
		return false;
	}
	
	@Override
	public void setValue(TCComponent tcc, int index, String propertyDisplayName) throws Exception {
		String value = getValue(index, propertyDisplayName)+ "";
		if (propertyDisplayName.equals("文档状态")) {
			MyStatusUtil.setStatus(tcc, value);
		} else if (propertyDisplayName.equals("所有者")) {
			String user_name = getValue(index, propertyDisplayName)+ "";
			if (user_name != null && !user_name.isEmpty()) {
				TCComponentUserType userType = (TCComponentUserType) session.getTypeComponent("User");
				TCComponentUser user = userType.find(user_name);
				if (user != null) {
					tcc.changeOwner(user, user.getLoginGroup());
				}
			}
		} else if (propertyDisplayName.equals("图文档分类ID")) {
			cls_manger.saveItemInNode(tcc, value);			
		} else if (propertyDisplayName.equals("电子档存放地址")) {
			if (value != null && value.length() > 0) {				
				File file = new File(value);
				if (file != null && file.exists() &&file.isFile()) {
					MyDatasetUtil.createDateset(tcc, file.getName(), file, "IMAN_specification");
				} else {
					throw new Exception("找不到数据集文件" + value);
				}
			} else {
				throw new Exception("电子档存放地址不能为空");
			}			
		} else if(!propertyDisplayName.equals("ID")){
			super.setValue(tcc, index, propertyDisplayName);
		}
	}
	
	public String newItemId(int index) throws Exception {
		String value = getValue(index, "图文档分类ID")+ "";
		if (value == null || value.length() == 0) {
			throw new Exception("图文档分类ID不能为空！");
		}
		return getID(value, 4);
	}
	
	public String getID(String prefix, int serialLength) throws Exception{
		String xml = proxy.getID(prefix, serialLength);
		XMLResult result = XMLResult.read(xml);
		String error = result.error;
		if (error != null && !error.isEmpty()) {
			throw new Exception(error);
		}
        return result.value;
	}
		
	@Override
	public void onPropertyRealNameNotFound(int index, String propertyName) {		
		System.out.println("第：" + index + "行的（" + propertyName + "）属性不存在！");
	}

	@Override
	public void onSetPropertyFinish(int index, String propertyDisplayName) {
		
	}

	@Override
	public void onSetPropertyError(int index, String propertyDisplayName, Exception e) {
		System.out.println("第" +index+ "行异常：" + e.getMessage());
	}

	@Override
	public boolean ignoreRow(int index) {
		return false;
	}

	@Override
	public boolean deleteOldItemWhenItemIdExist(int index) {
		return true;
	}
	@Override
	public void onSingleMessage(int index, String msg) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
