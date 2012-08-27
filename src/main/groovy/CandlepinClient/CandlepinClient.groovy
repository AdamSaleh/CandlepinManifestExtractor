package org.candlepin.groovy
import groovyx.net.http.*
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import static groovyx.net.http.ContentType.JSON

/**
 *
 * @author asaleh
 */
public class CandlepinClient {
    Boolean verbose = false
    def base_url
    def consumer
    def uuid
    JsonSlurper slurper = new JsonSlurper()
    public RESTClient client
     
    public CandlepinClient(username=null, password=null, 
        cert=null, key=null,
        host='localhost', port=8443, 
        context='candlepin',
        use_ssl = true){
             
        if(username!=null && cert!=null){
            throw new Exception("Cannot connect with both username and identity cert");
        }
        
        if(use_ssl){
            this.base_url = "https://$host:$port/$context/"
        }else{
            this.base_url = "http://$host:$port/$context/"
        }
        create_basic_client(username, password)   
        get("/candlepin/")
    }
    
    def create_product(product_id,product_name){
        def builder = new groovy.json.JsonBuilder(
            [ id:product_id
                , name:product_name
                , multiplier:1
                , attributes: []
                ,dependentProductIds: []
            ])
        post('products', builder.toString())
    }

    def get_product(String id) throws HttpResponseException{
        get('products/'+id);
    }
    def create_subscription(owner_key, product_id, quantity=1,
        contract_number='',
        account_number='',start_date=null,
        end_date=null){
        def builder = new groovy.json.JsonBuilder(
            [ startDate: "2012-07-31T00:00:00.0000",
                endDate: "2022-07-31T00:00:00.0000",
                quantity:  quantity,
                accountNumber: account_number,
                product: [ id :product_id ],
                providedProducts :[],
                contractNumber: contract_number
            ])
        post("owners/$owner_key/subscriptions", builder.toString())
    }
    
    def create_user(nlogin, npassword, nsuperadmin=false){
        def builder = new groovy.json.JsonBuilder(
            [
                superAdmin : nsuperadmin,
                username : nlogin,
                password : npassword
            ])

        post("users", builder.toString())
    }
   
    // TODO: need to switch to a params hash, getting to be too many arguments.
    def register(nname, ntype, owner_key=null ){
        def uuid=null;
        def nfacts=[:];
        def username=null
        def activation_keys=[];
        def ninstalledProducts=[];
        def environment=null;
        def builder = new groovy.json.JsonBuilder( [
                type : [label : ntype],
                name : nname,
                facts : nfacts,
                installedProducts : ninstalledProducts
            ])
        if(uuid!=null){
            consumer['uuid'] = uuid
        }
        def path =""
        def query_map = [:]
        if(environment==null){
            path = "consumers"
            if(owner_key!=null){
                query_map['owner']=owner_key
            }
        }else{
            path = "/environments/${environment}/consumers"
        }
        if(username!=null){
            query_map['username']=username
        }
      
        this.consumer = post(path, builder.toString(),query_map)
        return this.consumer
    }

    def consume_pool(npool,quantity){
        def path = "consumers/${this.uuid}/entitlements"
        def query = [pool:npool]
        if(quantity!=null){
            query['quantity']=quantity
        }
        raw_post(path,null,query)
    }
 
    def list_owner_pools(owner_id,product){
        def path = "pools"
        def query=[:]
        if(product!=null){
            query['owner']=owner_id
            query['product']=product
        }
        def results = get(path,query)

        return results
    }
  
    def async_control(status){
        while(status['state'].toLowerCase()!='finished'){
            status = post('/candlepin'+status['statusPath'])
        }
        return status['result']
    }
    
    def refresh_pools( owner_key,Boolean immediate=false,Boolean create_owner=false){
        def url = "owners/${owner_key}/subscriptions"
        if(create_owner){
            url += "?auto_create_owner=true" 
        }
        put(url)
    }
    
    def create_owner(owner_key){
        def builder = new groovy.json.JsonBuilder([key:owner_key,displayName:owner_key])
        post('owners', builder.toString())
    }
   
    def post(uri, data=null,query_map = null){
        def response = raw_post(uri, data,query_map)
        return response.getData()
    }
    
    def raw_post(uri, data=null,query_map = null){
        def response
        if(query_map!=null){
            response = client.post(path : uri,query : query_map, body : data, requestContentType: JSON)
        }else{
            response = client.post(path : uri, body : data, requestContentType: JSON)
        }
        return response
    }
    
    def put(uri, data=null){
        def response = client.put(path : uri, body : data, requestContentType: JSON)
        return response.getData();
    }
    
    def get(get_path,query_map = null) throws HttpResponseException{
        def response = raw_get(get_path,query_map)
        return response.getData();
    }
    
    def raw_get(get_path,query_map = null){
        def response
        if(query_map!=null){
            response = client.get(path : get_path,query : query_map)
        }else{
            response = client.get(path : get_path)
        }
        return response;
    }
    
    def create_basic_client(username=null,password=null){
        this.client = new RESTClient(this.base_url)
        this.client.auth.basic(username,password)
        this.client.headers[ 'Authorization' ] = "Basic "+
                 "$username:$password".getBytes('iso-8859-1').encodeBase64() 
    }
}

