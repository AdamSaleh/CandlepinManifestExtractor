package org.candlepin.groovy
import groovyx.net.http.*
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import java.security.KeyStore;
import java.security.KeyStore;
import org.candlepin.java.extractor.KeyCertConfig;
import static groovyx.net.http.ContentType.JSON

/**
 *
 * @author asaleh
 */
public class CandlepinClient {
    Boolean verbose = false
    String lang
    def links 
    def base_url
    def consumer
    def uuid
    JsonSlurper slurper = new JsonSlurper()
    Random random = new Random()
    public RESTClient client
    def identity_certificate
    
    
    
    public CandlepinClient(username=null, password=null, 
                            cert=null, key=null,
                 host='localhost', port=8443, 
                 lang=null, uuid=null,
                 trusted_user=false, context='candlepin',
                 use_ssl = true){
             
        if(username!=null && cert!=null){
            throw new Exception("Cannot connect with both username and identity cert");
        }
        
        if(use_ssl){
            this.base_url = "https://$host:$port/$context/"
        }else{
            this.base_url = "http://$host:$port/$context/"
        }
        
        this.lang = lang
        
        if(uuid!=null){
            create_trusted_consumer_client(uuid)
        } else if (trusted_user){
            create_trusted_user_client(username)
        } else if (cert!=null){
            create_ssl_client(cert, key)
        } else {
            create_basic_client(username, password)   
        }
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
    /*if(activation_keys.length > 0){
        path += "activation_keys=" + activation_keys.join(",") 
    }*/
    

    this.consumer = post(path, builder.toString(),query_map)
    return this.consumer
  }
  ///TODO: Should we change these to bind to better match terminology?
  def consume_pool(npool,quantity){
    def path = "consumers/${this.uuid}/entitlements"
    def query = [pool:npool]
    if(quantity!=null){
        query['quantity']=quantity
    }
    raw_post(path,null,query)
  }
 
   def list_owner_pools(owner_key,product){
    def path = "owners/$owner_key/pools"
    def query=[:]
    if(product!=null){
        query['product']=product
    }
    def results = get(path,query)

    return results
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
    def export_consumer(dest_dir){
        def path = "consumers/${this.uuid}/export"
        try{
        get_file(path, dest_dir)
        }catch(e){
            print e.message;
        }
      
    }
   //Assumes a zip archive currently. Returns filename (random#.zip) of the
  //temp file created.
  def get_file(uri, dest_dir){
    response = client.get(path:uri, accept: "application/zip")
    filename = response.getHeaders("content_disposition") == null ? "tmp_${random.nextiInt(1000)+1000}.zip" : response.getHeaders("content_disposition").split("filename=")[1]
    filename = File.join(dest_dir, filename)
    //File.open(filename, 'w') { |f| f.write(response.body) }
    def f1= new File(filename) << response.getData()
    return filename
   }
    def post(uri, data=null,query_map = null){
        def response
        if(query_map!=null){
            response = client.post(path : uri,query : query_map, body : data, requestContentType: JSON)
        }else{
            response = client.post(path : uri, body : data, requestContentType: JSON)
        }
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
    def get(get_path,query_map = null){
        def response
        if(query_map!=null){
            response = client.get(path : get_path,query : query_map)
        }else{
            response = client.get(path : get_path)
        }
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
    def create_ssl_client(cert, key){
        this.client = new RESTClient(this.base_url)
        KeyCertConfig config = new KeyCertConfig(this.client);
        
        this.client.setAuthConfig(config);
        config.certificate(key,cert,"asdf");
        this.uuid = config.getUuid();
       
    }
}

