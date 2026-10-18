"""One-time scaffolding of explicit JPA models and the matching baseline migration.
The generated sources are checked in; running this generator is not needed to build.
"""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
models = {
'UserAccount': ('user_account', 'email:str! fullName:str! passwordHash:str! enabled:bool! securityVersion:int!'),
'Role': ('app_role', 'code:str!'),
'UserRole': ('user_role', 'userId:long! roleId:long!'),
'Department': ('department', 'code:str! name:str! headUserId:long'),
'Student': ('student', 'userId:long! code:str! intake:str activeGroupId:long'),
'Lecturer': ('lecturer', 'userId:long! code:str! departmentId:long!'),
'RegistrationCycle': ('registration_cycle', 'name:str! cycleType:str! lecturerStart:time! lecturerEnd:time! studentStart:time! studentEnd:time! submissionDeadline:time! reviewerDeadline:time councilDate:time minMembers:int! maxMembers:int! reviewerCount:int! councilRequired:bool! status:str!'),
'Topic': ('topic', 'cycleId:long! departmentId:long! authorId:long! title:str! description:text! status:str! maxGroups:int! decisionNote:text approvedBy:long'),
'TopicSupervisor': ('topic_supervisor', 'topicId:long! lecturerId:long!'),
'StudentGroup': ('student_group', 'cycleId:long! name:str! leaderId:long! status:str!'),
'GroupMember': ('group_member', 'groupId:long! studentId:long! status:str!'),
'TopicRegistration': ('topic_registration', 'groupId:long! topicId:long! status:str! note:text decidedBy:long'),
'Submission': ('submission', 'registrationId:long! versionNo:int! storageKey:str! fileName:str! contentType:str! fileSize:long! submittedBy:long!'),
'ReviewerAssignment': ('reviewer_assignment', 'registrationId:long! lecturerId:long!'),
'ReviewCouncil': ('review_council', 'cycleId:long! name:str! reportAt:time! location:str!'),
'CouncilMember': ('council_member', 'councilId:long! lecturerId:long! memberRole:str!'),
'CouncilAssignment': ('council_assignment', 'councilId:long! registrationId:long! confirmedBy:long confirmedAt:time'),
'Evaluation': ('evaluation', 'registrationId:long! lecturerId:long! evaluationType:str! score:decimal comment:text status:str!'),
'EvaluationResult': ('evaluation_result', 'registrationId:long! finalScore:decimal! status:str! publicationVersion:int! publishedBy:long! publishedAt:time!'),
'Announcement': ('announcement', 'title:str! content:text! authorId:long! audience:str! departmentId:long status:str!'),
'EmailDelivery': ('email_delivery', 'resultId:long! publicationVersion:int! recipientId:long! recipient:str! subject:str! body:text! status:str! attempts:int! lastError:str'),
'AuditLog': ('audit_log', 'actorId:long action:str! objectType:str! objectId:long details:text'),
}
unique = {'UserAccount':['email'],'Role':['code'],'UserRole':['user_id, role_id'],'Department':['code'], 'Student':['user_id','code'],'Lecturer':['user_id','code'],'TopicSupervisor':['topic_id, lecturer_id'], 'GroupMember':['group_id, student_id'],'Submission':['registration_id, version_no'],'ReviewerAssignment':['registration_id, lecturer_id'],'CouncilMember':['council_id, lecturer_id'],'CouncilAssignment':['registration_id'],'Evaluation':['registration_id, lecturer_id'],'EvaluationResult':['registration_id'],'EmailDelivery':['result_id, publication_version, recipient_id']}
types={'str':('String','VARCHAR(255)'), 'text':('String','TEXT'),'long':('Long','BIGINT'),'int':('Integer','INT'),'bool':('Boolean','BOOLEAN'),'time':('LocalDateTime','DATETIME(6)'),'decimal':('BigDecimal','DECIMAL(5,2)')}
def snake(s): return re.sub(r'(?<!^)(?=[A-Z])','_',s).lower()
def write(p,s):
    p=root/p; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(s,encoding='utf-8')
sql=[]
for cls,(table,fields) in models.items():
    columns=['id BIGINT AUTO_INCREMENT PRIMARY KEY','version BIGINT NOT NULL DEFAULT 0','created_at DATETIME(6) NOT NULL']
    java=[]
    for field in fields.split():
        name,typ=field.split(':'); required=typ.endswith('!');typ=typ.rstrip('!'); jt,st=types[typ]
        col=snake(name); columns.append(f'{col} {st}'+(' NOT NULL' if required else ''))
        args=f'name="{col}", nullable={str(not required).lower()}'
        if typ=='text':args+=', columnDefinition="TEXT"'
        if typ=='decimal':args+=', precision=5, scale=2'
        java.append(f'    @Column({args})\n    private {jt} {name};')
    for u in unique.get(cls,[]): columns.append(f'UNIQUE ({u})')
    sql.append(f'CREATE TABLE {table} (\n    '+',\n    '.join(columns)+'\n);')
    write(Path(f'src/main/java/vn/edu/detaihub/model/{cls}.java'),f'''package vn.edu.detaihub.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.math.BigDecimal;
@Entity @Table(name="{table}") @Getter @Setter
public class {cls} extends BaseEntity {{
'''+ '\n'.join(java)+'\n}\n')
# Explicit FK constraints, including the membership-pointer cycle added after all tables exist.
refs={
'user_role':{'user_id':'user_account','role_id':'app_role'},'department':{'head_user_id':'user_account'},
'student':{'user_id':'user_account','active_group_id':'student_group'},'lecturer':{'user_id':'user_account','department_id':'department'},
'topic':{'cycle_id':'registration_cycle','department_id':'department','author_id':'user_account','approved_by':'user_account'},
'topic_supervisor':{'topic_id':'topic','lecturer_id':'lecturer'},'student_group':{'cycle_id':'registration_cycle','leader_id':'student'},
'group_member':{'group_id':'student_group','student_id':'student'},'topic_registration':{'group_id':'student_group','topic_id':'topic','decided_by':'user_account'},
'submission':{'registration_id':'topic_registration','submitted_by':'user_account'},'reviewer_assignment':{'registration_id':'topic_registration','lecturer_id':'lecturer'},
'review_council':{'cycle_id':'registration_cycle'},'council_member':{'council_id':'review_council','lecturer_id':'lecturer'},
'council_assignment':{'council_id':'review_council','registration_id':'topic_registration','confirmed_by':'user_account'},
'evaluation':{'registration_id':'topic_registration','lecturer_id':'lecturer'},'evaluation_result':{'registration_id':'topic_registration','published_by':'user_account'},
'announcement':{'author_id':'user_account','department_id':'department'},'email_delivery':{'result_id':'evaluation_result','recipient_id':'user_account'},'audit_log':{'actor_id':'user_account'}}
for table,fks in refs.items():
    for col,target in fks.items():sql.append(f'ALTER TABLE {table} ADD CONSTRAINT fk_{table}_{col} FOREIGN KEY ({col}) REFERENCES {target}(id);')
sql.extend(['ALTER TABLE evaluation ADD CONSTRAINT ck_score CHECK (score IS NULL OR (score >= 0 AND score <= 10));','ALTER TABLE registration_cycle ADD CONSTRAINT ck_members CHECK (min_members >= 3 AND max_members >= min_members AND max_members <= 5);'])
write(Path('src/main/resources/db/migration/V1__schema.sql'),'\n\n'.join(sql)+'\n')
write(Path('src/main/java/vn/edu/detaihub/model/BaseEntity.java'),'''package vn.edu.detaihub.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
@MappedSuperclass @Getter @Setter
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @Column(nullable=false, updatable=false) private LocalDateTime createdAt;
    @PrePersist void stamp() { createdAt=LocalDateTime.now(ZoneOffset.UTC); }
}
''')
print(f'Generated {len(models)} models and relational migration.')
